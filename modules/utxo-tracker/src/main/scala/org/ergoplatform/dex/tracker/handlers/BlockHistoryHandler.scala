package org.ergoplatform.dex.tracker.handlers

import cats.effect.Clock
import cats.{Functor, FunctorFilter, Monad}
import mouse.all.anySyntaxMouse
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.common.streaming.{Producer, Record}
import org.ergoplatform.ergo.BlockId
import org.ergoplatform.ergo.domain.SettledBlock
import tofu.logging.{Logging, Logs}
import tofu.streams.Evals
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.streams.all._

final class BlockHistoryHandler[
  F[_]: Monad: Evals[*[_], G]: FunctorFilter,
  G[_]: Monad: Logging
](implicit
  producer: Producer[BlockId, SettledBlock, F]
) {

  def handler: SettledBlockHandler[F] =
    _.evalTap(op => info"Evaluated CFMM operation detected $op")
      .map(op => Record[BlockId, SettledBlock](BlockId(op.id), op))
      .thrush(producer.produce)
}

object BlockHistoryHandler {

  def make[
    I[_]: Functor,
    F[_]: Monad: Evals[*[_], G]: FunctorFilter,
    G[_]: Monad: Clock
  ](implicit
    producer: Producer[BlockId, SettledBlock, F],
    logs: Logs[I, G],
    e: ErgoAddressEncoder
  ): I[SettledBlockHandler[F]] =
    logs.forService[BlockHistoryHandler[F, G]].map { implicit log =>
      new BlockHistoryHandler[F, G].handler
    }
}
