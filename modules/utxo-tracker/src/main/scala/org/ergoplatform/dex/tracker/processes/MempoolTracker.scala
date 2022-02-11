package org.ergoplatform.dex.tracker.processes

import cats.effect.Clock
import cats.{Defer, FlatMap, Monad, MonoidK}
import org.ergoplatform.common.data.TemporalFilter
import org.ergoplatform.dex.tracker.configs.MempoolTrackingConfig
import org.ergoplatform.dex.tracker.handlers.BoxHandler
import org.ergoplatform.ergo.modules.MempoolStreaming
import tofu.Catches
import tofu.concurrent.MakeRef
import tofu.logging.{Logging, Logs}
import tofu.streams.{Evals, Pace, ParFlatten}
import tofu.syntax.embed._
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.streams.all._

import scala.concurrent.duration._

/** Tracks UTxOs from mempool.
  */
final class MempoolTracker[
  F[_]: Monad: Evals[*[_], G]: ParFlatten: Pace: Defer: MonoidK: Catches,
  G[_]: Monad: Logging
](conf: MempoolTrackingConfig, filter: TemporalFilter[G], handlers: List[BoxHandler[F]])(implicit
  mempool: MempoolStreaming[F]
) extends UtxoTracker[F] {

  def run: F[Unit] = {
    def sync: F[Unit] =
      for {
        output  <- mempool.streamUnspentOutputs
        unknown <- eval(filter.probe(output.boxId))
        _ <- if (unknown) emits(handlers.map(_(output.pure[F]))).parFlattenUnbounded
             else unit[F]
        _ <- sync.delay(conf.samplingInterval)
      } yield ()
    eval(info"Starting Mempool Tracker ..") >> sync
  }
}

object MempoolTracker {

  def make[
    I[_]: FlatMap,
    F[_]: Monad: Evals[*[_], G]: ParFlatten: Pace: Defer: MonoidK: MempoolTrackingConfig.Has: Catches,
    G[_]: Monad: Clock
  ](handlers: BoxHandler[F]*)(implicit
    mempool: MempoolStreaming[F],
    logs: Logs[I, G],
    makeRef: MakeRef[I, G]
  ): I[UtxoTracker[F]] =
    for {
      implicit0(l: Logging[G]) <- logs.forService[MempoolTracker[F, G]]
      filter                   <- TemporalFilter.make[I, G](30.minutes, 12)
      tracker = MempoolTrackingConfig.access
                  .map(conf => new MempoolTracker[F, G](conf, filter, handlers.toList): UtxoTracker[F])
                  .embed
    } yield tracker
}
