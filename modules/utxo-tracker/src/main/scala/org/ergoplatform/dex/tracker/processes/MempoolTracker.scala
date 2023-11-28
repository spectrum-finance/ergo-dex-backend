package org.ergoplatform.dex.tracker.processes

import cats.effect.{Clock, Timer}
import cats.instances.list._
import cats.{Defer, FlatMap, Monad, MonoidK}
import org.ergoplatform.dex.tracker.handlers.BoxHandler
import org.ergoplatform.dex.tracker.streaming.{MempoolConsumer, MempoolEvent}
import tofu.Catches
import tofu.logging.{Logging, Logs}
import tofu.streams.{Evals, Pace, ParFlatten}
import tofu.syntax.handle._
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.streams.all.{eval, _}

/** Tracks UTxOs from mempool.
  */
final class MempoolTracker[
  F[_]: Monad: Evals[*[_], G]: ParFlatten: Pace: Defer: MonoidK: Catches,
  G[_]: Monad: Logging: Timer
](handlers: List[BoxHandler[F]], consumer: MempoolConsumer[F, G])
  extends UtxoTracker[F] {

  def run: F[Unit] =
    consumer.stream
      .evalMap { mempoolEvent =>
        mempoolEvent.message match {
          case Some(MempoolEvent.MempoolApply(transaction)) =>
            eval(info"Scanning unconfirmed tx ${transaction.id}") >>
              emits(transaction.outputs.map { out =>
                eval(debug"Scanning unconfirmed output ${out.boxId}") >>
                emits(handlers.map(_(out.pure[F]))).parFlattenUnbounded
              }).parFlattenUnbounded >> eval(mempoolEvent.commit)
          case _ => eval(mempoolEvent.commit)
        }
      }
      .handleWith[Throwable](e => eval(warnCause"Mempool Tracker failed, restarting .." (e)) >> run)
}

object MempoolTracker {

  def make[
    I[_]: FlatMap,
    F[_]: Monad: Evals[*[_], G]: ParFlatten: Pace: Defer: MonoidK: Catches,
    G[_]: Monad: Clock: Timer
  ](consumer: MempoolConsumer[F, G], handlers: BoxHandler[F]*)(implicit logs: Logs[I, G]): I[UtxoTracker[F]] =
    for {
      implicit0(l: Logging[G]) <- logs.forService[MempoolTracker[F, G]]
    } yield new MempoolTracker[F, G](handlers.toList, consumer): UtxoTracker[F]
}
