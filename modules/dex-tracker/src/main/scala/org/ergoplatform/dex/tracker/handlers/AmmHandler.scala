package org.ergoplatform.dex.tracker.handlers

import cats.{Functor, FunctorFilter, Monad}
import mouse.any._
import org.ergoplatform.dex.domain.amm.{CfmmOperation, Deposit, OperationId, Redeem, Swap}
import org.ergoplatform.dex.protocol.amm.{AmmContractType, AmmContracts}
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
  contracts: AmmContracts[CT]
) {

  def handler: BoxHandler[F] =
    _.map { out =>
      if (contracts.isDeposit(out.ergoTree)) Some(Deposit(???, out))
      else if (contracts.isRedeem(out.ergoTree)) Some(Redeem(???, out))
      else if (contracts.isSwap(out.ergoTree)) Some(Swap(???, out))
      else Option.empty[CfmmOperation]
    }.unNone
      .evalTap(op => info"AMM operation detected $op")
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
    contracts: AmmContracts[CT],
    logs: Logs[I, G]
  ): I[BoxHandler[F]] =
    logs.forService[AmmHandler[CT, F, G]].map(implicit log => new AmmHandler[CT, F, G].handler)
}