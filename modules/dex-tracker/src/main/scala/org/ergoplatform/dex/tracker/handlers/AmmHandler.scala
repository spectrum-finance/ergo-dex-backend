package org.ergoplatform.dex.tracker.handlers

import cats.implicits.none
import cats.{Functor, FunctorFilter, Monad}
import mouse.any._
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.protocol.amm.{AmmContractType, AmmOps}
import org.ergoplatform.dex.streaming.{Producer, Record}
import tofu.logging.{Logging, Logs}
import tofu.streams.Evals
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.streams.all._

final class AmmHandler[
  CT <: AmmContractType,
  F[_]: Monad: Evals[*[_], G]: FunctorFilter,
  G[_]: Functor: Logging
](implicit
  producer: Producer[OperationId, CfmmOperation, F],
  ops: AmmOps[CT]
) {

  def handler: BoxHandler[F] =
    _.map { out =>
      ops.parseDeposit(out) orElse
      ops.parseRedeem(out) orElse
      ops.parseSwap(out) orElse
      none[CfmmOperation]
    }.unNone
      .evalTap(op => info"AMM operation detected [$op]")
      .map(op => Record[OperationId, CfmmOperation](op.id, op))
      .thrush(producer.produce)
}

object AmmHandler {

  def make[
    CT <: AmmContractType,
    I[_]: Functor,
    F[_]: Monad: Evals[*[_], G]: FunctorFilter,
    G[_]: Functor
  ](implicit
    producer: Producer[OperationId, CfmmOperation, F],
    contracts: AmmOps[CT],
    logs: Logs[I, G]
  ): I[BoxHandler[F]] =
    logs.forService[AmmHandler[CT, F, G]].map(implicit log => new AmmHandler[CT, F, G].handler)
}
