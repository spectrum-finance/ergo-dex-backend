package org.ergoplatform.dex.markets.processes

import cats.effect.Clock
import cats.implicits.none
import cats.instances.list._
import cats.syntax.traverse._
import cats.{Functor, FunctorFilter, Monad}
import mouse.all.anySyntaxMouse
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.common.streaming.{Producer, Record}
import org.ergoplatform.ergo.domain.SettledBlock
import tofu.logging.{Logging, Logs}
import tofu.streams.Evals
import tofu.syntax.foption._
import tofu.syntax.streams.all._
import tofu.syntax.monadic._
import tofu.syntax.logging._

final class BlockHistoryHandler[
  F[_]: Monad: Evals[*[_], G]: FunctorFilter,
  G[_]: Monad: Logging
](implicit
  producer: Producer[Int, SettledBlock, F]
) {

  def handler: F[SettledBlock] => F[Unit] =
    _.evalTap(op => info"Evaluated CFMM operation detected $op")
      .map(op => Record[Int, SettledBlock](op.height, op))
      .thrush(producer.produce)
}

object BlockHistoryHandler {

  def make[
    I[_]: Functor,
    F[_]: Monad: Evals[*[_], G]: FunctorFilter,
    G[_]: Monad: Clock
  ](implicit
    producer: Producer[Int, SettledBlock, F],
    logs: Logs[I, G],
    e: ErgoAddressEncoder
  ): I[F[SettledBlock] => F[Unit]] =
    logs.forService[BlockHistoryHandler[F, G]].map { implicit log =>
      new BlockHistoryHandler[F, G].handler
    }
}
