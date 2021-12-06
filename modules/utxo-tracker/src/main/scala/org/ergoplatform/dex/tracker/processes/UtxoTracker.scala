package org.ergoplatform.dex.tracker.processes

import cats.{Defer, FlatMap, Monad, MonoidK}
import derevo.derive
import org.ergoplatform.dex.tracker.configs.UtxoTrackerConfig
import org.ergoplatform.dex.tracker.handlers.BoxHandler
import org.ergoplatform.dex.tracker.repositories.TrackerCache
import org.ergoplatform.ergo.ErgoNetworkStreaming
import tofu.Catches
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.streams.{Evals, Pace, ParFlatten}
import tofu.syntax.context._
import tofu.syntax.embed._
import tofu.syntax.handle._
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.streams.all._

@derive(representableK)
trait UtxoTracker[F[_]] {

  def run: F[Unit]
}

object UtxoTracker {

  trait TrackerMode

  object TrackerMode {
    // Track only unspent outputs
    object Live extends TrackerMode
    // Track only spent outputs since the very beginning of blockchain history
    object Historical extends TrackerMode
  }

  def make[
    I[_]: FlatMap,
    F[_]: Monad: Evals[*[_], G]: ParFlatten: Pace: Defer: MonoidK: UtxoTrackerConfig.Has: Catches,
    G[_]: Monad
  ](mode: TrackerMode, trackers: BoxHandler[F]*)(implicit
    client: ErgoNetworkStreaming[F, G],
    cache: TrackerCache[G],
    logs: Logs[I, G]
  ): I[UtxoTracker[F]] =
    logs.forService[UtxoTracker[F]].map { implicit l =>
      (context map
      (conf => new StreamingTracker[F, G](mode, cache, conf, trackers.toList): UtxoTracker[F])).embed
    }

  final private[dex] class StreamingTracker[
    F[_]: Monad: Evals[*[_], G]: ParFlatten: Pace: Defer: MonoidK: Catches,
    G[_]: Monad: Logging
  ](mode: TrackerMode, cache: TrackerCache[G], conf: UtxoTrackerConfig, handlers: List[BoxHandler[F]])(implicit
    client: ErgoNetworkStreaming[F, G]
  ) extends UtxoTracker[F] {

    def run: F[Unit] =
      eval(info"Starting UTXO tracker in mode [${mode.toString}] ..") >>
      eval(cache.lastScannedBoxOffset).repeat
        .flatMap { lastOffset =>
          eval(client.getNetworkInfo).flatMap { networkParams =>
            val offset     = lastOffset max conf.initialOffset
            val maxOffset  = networkParams.maxBoxGix
            val nextOffset = (offset + conf.batchSize) min maxOffset
            val outputsStream = mode match {
              case TrackerMode.Historical => client.streamOutputs(offset, conf.batchSize)
              case TrackerMode.Live       => client.streamUnspentOutputs(offset, conf.batchSize)
            }
            val scan =
              eval(info"Requesting UTXO batch {offset=$offset, maxOffset=$maxOffset, batchSize=${conf.batchSize} ..") >>
              outputsStream
                .evalTap(out => trace"Scanning box $out")
                .flatTap(out => emits(handlers.map(_(out.pure[F]))).parFlattenUnbounded)
                .evalMap(out => cache.setLastScannedBoxOffset(out.globalIndex))
            val finalizeOffset = eval(cache.setLastScannedBoxOffset(nextOffset))
            val pause =
              eval(info"Upper limit {maxOffset=$maxOffset} was reached. Retrying in ${conf.retryDelay.toSeconds}s") >>
              unit[F].delay(conf.retryDelay)

            emits(if (offset != maxOffset) List(scan, finalizeOffset) else List(pause)).flatten
          }
        }
        .handleWith[Throwable] { e =>
          val delay = conf.retryDelay
          eval(warnCause"Tracker failed. Retrying in $delay ms" (e)) >> run.delay(delay)
        }
  }
}
