package org.ergoplatform.dex.tracker.handlers

import cats.implicits.none
import cats.{Functor, FunctorFilter, Monad}
import mouse.any._
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.protocol.amm.AMMType.CFMMFamily
import org.ergoplatform.common.streaming.{Producer, Record}
import org.ergoplatform.dex.tracker.parsers.amm.AMMOpsParser
import org.ergoplatform.dex.tracker.validation.amm.CFMMRules
import tofu.logging.{Logging, Logs}
import tofu.streams.Evals
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.streams.all._

final class CFMMOpsHandler[
  CT <: CFMMFamily,
  F[_]: Monad: Evals[*[_], G]: FunctorFilter,
  G[_]: Functor: Logging
](implicit
  producer: Producer[OperationId, CFMMOperationRequest, F],
  parser: AMMOpsParser[CT],
  rules: CFMMRules[G]
) {

  def handler: BoxHandler[F] =
    _.map { out =>
      parser.deposit(out) orElse
      parser.redeem(out) orElse
      parser.swap(out) orElse
      none[CFMMOperationRequest]
    }.unNone
      .evalTap(op => info"CFMM operation request detected [$op]")
      .flatMap(op => eval(rules(op)).ifM(op.pure, Evals[F, G].monoidK.empty))
      .map(op => Record[OperationId, CFMMOperationRequest](op.id, op))
      .thrush(producer.produce)
}

object CFMMOpsHandler {

  def make[
    CT <: CFMMFamily,
    I[_]: Functor,
    F[_]: Monad: Evals[*[_], G]: FunctorFilter,
    G[_]: Functor
  ](implicit
    producer: Producer[OperationId, CFMMOperationRequest, F],
    contracts: AMMOpsParser[CT],
    rules: CFMMRules[G],
    logs: Logs[I, G]
  ): I[BoxHandler[F]] =
    logs.forService[CFMMOpsHandler[CT, F, G]].map(implicit log => new CFMMOpsHandler[CT, F, G].handler)
}
