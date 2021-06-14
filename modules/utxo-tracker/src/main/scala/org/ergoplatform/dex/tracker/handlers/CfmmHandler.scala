package org.ergoplatform.dex.tracker.handlers

import cats.implicits.none
import cats.{Functor, FunctorFilter, Monad}
import mouse.any._
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.protocol.amm.AmmContractType.CfmmFamily
import org.ergoplatform.common.streaming.{Producer, Record}
import org.ergoplatform.dex.tracker.parsers.amm.AmmOps
import org.ergoplatform.dex.tracker.validation.amm.CfmmRules
import tofu.logging.{Logging, Logs}
import tofu.streams.Evals
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.streams.all._

final class CfmmHandler[
  CT <: CfmmFamily,
  F[_]: Monad: Evals[*[_], G]: FunctorFilter,
  G[_]: Functor: Logging
](implicit
  producer: Producer[OperationId, CfmmOperation, F],
  ops: AmmOps[CT],
  rules: CfmmRules[G]
) {

  def handler: BoxHandler[F] =
    _.map { out =>
      ops.parseDeposit(out) orElse
      ops.parseRedeem(out) orElse
      ops.parseSwap(out) orElse
      none[CfmmOperation]
    }.unNone
      .evalTap(op => info"AMM operation detected [$op]")
      .flatMap(op => eval(rules(op)).ifM(op.pure, Evals[F, G].monoidK.empty))
      .map(op => Record[OperationId, CfmmOperation](op.id, op))
      .thrush(producer.produce)
}

object CfmmHandler {

  def make[
    CT <: CfmmFamily,
    I[_]: Functor,
    F[_]: Monad: Evals[*[_], G]: FunctorFilter,
    G[_]: Functor
  ](implicit
    producer: Producer[OperationId, CfmmOperation, F],
    contracts: AmmOps[CT],
    rules: CfmmRules[G],
    logs: Logs[I, G]
  ): I[BoxHandler[F]] =
    logs.forService[CfmmHandler[CT, F, G]].map(implicit log => new CfmmHandler[CT, F, G].handler)
}
