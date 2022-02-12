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
import tofu.syntax.handle._
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
      (for {
        _       <- eval(debug"Tick")
        output  <- mempool.streamUnspentOutputs
        known   <- eval(filter.probe(output.boxId))
        (n, mx) <- eval(filter.inspect)
        _       <- eval(debug"Filter{N=$n, MX=$mx}")
        _ <- if (!known)
               eval(debug"New unconfirmed output discovered: $output")
             else unit[F]
      } yield ()) >> eval(debug"Going to wait ${conf.samplingInterval}") >> unit[F].delay(conf.samplingInterval) >> eval(debug"Done, starting new loop")  >> sync
    eval(info"Starting Mempool Tracker ..") >>
    sync.handleWith[Throwable](e => eval(warnCause"Mempool Tracker failed, restarting .." (e)) >> run)
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
