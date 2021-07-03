package org.ergoplatform.dex.tracker.processes

import cats.{Defer, FlatMap, Monad, MonoidK}
import derevo.derive
import mouse.any._
import org.ergoplatform.dex.tracker.configs.TrackerConfig
import org.ergoplatform.dex.tracker.handlers.BoxHandler
import org.ergoplatform.dex.tracker.repositories.TrackerCache
import org.ergoplatform.ergo.StreamingErgoNetworkClient
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

  def make[
    I[_]: FlatMap,
    F[_]: Monad: Evals[*[_], G]: ParFlatten: Pace: Defer: MonoidK: TrackerConfig.Has: Catches,
    G[_]: Monad
  ](trackers: BoxHandler[F]*)(implicit
    client: StreamingErgoNetworkClient[F, G],
    cache: TrackerCache[G],
    logs: Logs[I, G]
  ): I[UtxoTracker[F]] =
    logs.forService[UtxoTracker[F]].map { implicit l =>
      (context map (conf => new Live[F, G](cache, conf, trackers.toList): UtxoTracker[F])).embed
    }

  final private class Live[
    F[_]: Monad: Evals[*[_], G]: ParFlatten: Pace: Defer: MonoidK: Catches,
    G[_]: Monad: Logging
  ](cache: TrackerCache[G], conf: TrackerConfig, handlers: List[BoxHandler[F]])(implicit
    client: StreamingErgoNetworkClient[F, G]
  ) extends UtxoTracker[F] {

    def run: F[Unit] =
      eval(info"Starting tracking ..") >>
      eval(cache.lastScannedBoxOffset).repeat
        .flatMap { lastOffset =>
          val offset = lastOffset max conf.initialOffset
          val process =
            client
              .streamUnspentOutputs(offset, conf.batchSize)
              .evalTap(out => trace"Scanning box $out")
              .flatTap(out => emits(handlers.map(_(out.pure[F]))).parFlattenUnbounded)
              .evalMap(out => cache.setLastScannedBoxOffset(out.globalIndex))
          val finalizeOffset = eval(cache.setLastScannedBoxOffset(offset + conf.batchSize))
          eval(info"Requesting UTXO batch {offset=$offset, batchSize=${conf.batchSize} ..") >>
          emits(List(process, finalizeOffset)).flatten
        }
        .handleWith[Throwable] { e =>
          val delay = conf.retryDelay
          eval(warnCause"Tracker failed. Retrying in $delay ms" (e)) >> run.delay(delay)
        }
  }
}
