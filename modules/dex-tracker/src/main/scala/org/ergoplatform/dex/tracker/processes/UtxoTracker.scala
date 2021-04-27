package org.ergoplatform.dex.tracker.processes

import cats.effect.concurrent.Ref
import cats.{Defer, FlatMap, Monad, MonoidK}
import derevo.derive
import mouse.any._
import org.ergoplatform.dex.clients.StreamingErgoNetworkClient
import org.ergoplatform.dex.clients.explorer.models.Output
import org.ergoplatform.dex.tracker.configs.TrackerConfig
import org.ergoplatform.dex.tracker.domain.errors.InvalidOrder
import tofu.concurrent.MakeRef
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.streams.{Broadcast, Evals, Pace}
import tofu.syntax.context._
import tofu.syntax.embed._
import tofu.syntax.handle._
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.streams.all._
import tofu.{Catches, Handle}

@derive(representableK)
trait UtxoTracker[F[_]] {

  def run: F[Unit]
}

object UtxoTracker {

  def make[
    I[_]: FlatMap,
    F[_]: Monad: Evals[*[_], G]: Broadcast: Pace: Defer: MonoidK: TrackerConfig.Has: Catches,
    G[_]: Monad: Handle[*[_], InvalidOrder]
  ](trackers: BoxHandler[F]*)(implicit
    client: StreamingErgoNetworkClient[F, G],
    logs: Logs[I, G],
    makeRef: MakeRef[I, G]
  ): I[UtxoTracker[F]] =
    logs.forService[UtxoTracker[F]].flatMap { implicit l =>
      makeRef.refOf(0).map { ref =>
        (context map (conf => new Live[F, G](ref, conf, trackers.toList): UtxoTracker[F])).embed
      }
    }

  final private class Live[
    F[_]: Monad: Evals[*[_], G]: Broadcast: Pace: Defer: MonoidK: Catches,
    G[_]: Monad: Handle[*[_], InvalidOrder]: Logging
  ](lastScannedHeightRef: Ref[G, Int], conf: TrackerConfig, handlers: List[BoxHandler[F]])(implicit
    client: StreamingErgoNetworkClient[F, G]
  ) extends UtxoTracker[F] {

    def run: F[Unit] =
      eval(client.getCurrentHeight).repeat
        .throttled(conf.scanInterval)
        .flatMap { height =>
          eval(lastScannedHeightRef.get)
            .evalTap { lastScannedHeight =>
              if (lastScannedHeight < height) info"Checking height delta ($lastScannedHeight, $height)"
              else info"Waiting for new blocks. Current height is [$height]"
            }
            .flatMap { lastScannedHeight =>
              val lastEpochs = (height - lastScannedHeight) min conf.scanLastEpochs
              if (lastEpochs > 0) client.streamUnspentOutputs(lastEpochs)
              else MonoidK[F].empty[Output]
            }
            .flatTap(_ => eval(lastScannedHeightRef.set(height)))
        }
        .evalTap(out => trace"Scanning box $out")
        .broadcast(handlers: _*)
        .handleWith[Throwable] { e =>
          val delay = conf.retryDelay
          eval(warnCause"Tracker failed. Retrying in $delay ms" (e)) >> run.delay(delay)
        }
  }
}
