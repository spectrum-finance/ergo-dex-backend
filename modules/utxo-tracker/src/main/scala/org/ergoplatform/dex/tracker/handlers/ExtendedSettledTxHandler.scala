package org.ergoplatform.dex.tracker.handlers

import cats.{Functor, FunctorFilter, Monad}
import mouse.all._
import org.ergoplatform.common.streaming.{Producer, Record}
import org.ergoplatform.ergo.TxId
import org.ergoplatform.ergo.domain.ExtendedSettledTx
import tofu.logging.{Logging, Logs}
import tofu.streams.Evals
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.streams.evals._

final class ExtendedSettledTxHandler[
  F[_]: Monad: Evals[*[_], G]: FunctorFilter,
  G[_]: Functor: Logging
](implicit
  producer: Producer[TxId, ExtendedSettledTx, F]
) {

  def handler: ExtendedTxHandler[F] =
    _.evalTap(tx => trace"Got next settled tx: ${tx.id}.")
      .map(tx => Record[TxId, ExtendedSettledTx](tx.id, tx))
      .thrush(producer.produce)

}

object ExtendedSettledTxHandler {

  def make[
    I[_]: Functor,
    F[_]: Monad: Evals[*[_], G]: FunctorFilter,
    G[_]: Functor
  ](implicit
    producer: Producer[TxId, ExtendedSettledTx, F],
    logs: Logs[I, G]
  ): I[ExtendedTxHandler[F]] =
    logs.forService[SettledCFMMPoolsHandler[F, G]].map { implicit log =>
      new ExtendedSettledTxHandler[F, G].handler
    }
}
