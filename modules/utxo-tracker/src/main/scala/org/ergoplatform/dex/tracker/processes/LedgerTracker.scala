package org.ergoplatform.dex.tracker.processes

import cats.{Defer, FlatMap, Monad, MonoidK}
import org.ergoplatform.dex.tracker.configs.LedgerTrackingConfig
import org.ergoplatform.dex.tracker.handlers.SettledBoxHandler
import org.ergoplatform.dex.tracker.streaming.{TransactionConsumer, TransactionEvent}
import org.ergoplatform.ergo.domain.SettledOutput
import tofu.Catches
import tofu.logging.{Logging, Logs}
import tofu.streams.{Evals, Pace, ParFlatten}
import tofu.syntax.handle._
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.streams.all._

final class LedgerTracker[
  F[_]: Monad: Evals[*[_], G]: ParFlatten: Pace: Defer: MonoidK: Catches,
  G[_]: Monad: Logging
](consumer: TransactionConsumer[F, G], handlers: List[SettledBoxHandler[F]])
  extends UtxoTracker[F] {

  def run: F[Unit] =
    eval(info"Starting Ledger Tracker ..") >>
    consumer.stream
      .evalMap { txEvent =>
        txEvent.message match {
          case Some(TransactionEvent.TransactionApply(transaction, _, h)) =>
            eval(info"Scanning tx ${transaction.id}") >>
              emits(
                transaction.outputs
                  .map(out => SettledOutput(out, h, h))
                  .map { out =>
                    eval(debug"Scanning output ${out.output.boxId}") >>
                    emits(handlers.map(_(out.pure[F]))).parFlattenUnbounded
                  }
              ).parFlattenUnbounded >> eval(txEvent.commit)
          case _ => eval(txEvent.commit)
        }
      }
      .handleWith[Throwable](e => eval(warnCause"Ledger Tracker failed, restarting .." (e)) >> run)
}

object LedgerTracker {

  sealed trait TrackerMode

  object TrackerMode {
    // Track only unspent outputs
    object Live extends TrackerMode
    // Track only spent outputs since the very beginning of blockchain history
    object Historical extends TrackerMode
  }

  def make[
    I[_]: FlatMap,
    F[_]: Monad: Evals[*[_], G]: ParFlatten: Pace: Defer: MonoidK: LedgerTrackingConfig.Has: Catches,
    G[_]: Monad
  ](consumer: TransactionConsumer[F, G], handlers: SettledBoxHandler[F]*)(implicit
    logs: Logs[I, G]
  ): I[UtxoTracker[F]] =
    logs.forService[LedgerTracker[F, G]].map { implicit l =>
      new LedgerTracker[F, G](consumer, handlers.toList): UtxoTracker[F]
    }
}
