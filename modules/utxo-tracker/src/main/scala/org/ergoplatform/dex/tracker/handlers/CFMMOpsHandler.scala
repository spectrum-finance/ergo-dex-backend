package org.ergoplatform.dex.tracker.handlers

import cats.implicits.none
import cats.{FlatMap, Functor, FunctorFilter, Monad}
import mouse.any._
import org.ergoplatform.common.streaming.{Producer, Record}
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.protocol.amm.AMMType.CFMMFamily
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
  G[_]: FlatMap: Logging
](implicit
  producer: Producer[OrderId, CFMMOrder, F],
  parser: AMMOpsParser[CT, G],
  rules: CFMMRules[G]
) {

  def handler: BoxHandler[F] =
    _.evalMap { out =>
      for {
        deposit <- parser.deposit(out)
        redeem  <- parser.redeem(out)
        swap    <- parser.swap(out)
      } yield deposit orElse redeem orElse swap orElse none[CFMMOrder]
    }.unNone
      .evalTap(op => info"CFMM operation request detected $op")
      .flatMap { op =>
        eval(rules(op)) >>=
          (_.fold(op.pure)(e => eval(debug"Rule violation: $e") >> Evals[F, G].monoidK.empty))
      }
      .map(op => Record[OrderId, CFMMOrder](op.id, op))
      .thrush(producer.produce)
}

object CFMMOpsHandler {

  def make[
    CT <: CFMMFamily,
    I[_]: Functor,
    F[_]: Monad: Evals[*[_], G]: FunctorFilter,
    G[_]: FlatMap
  ](implicit
    producer: Producer[OrderId, CFMMOrder, F],
    contracts: AMMOpsParser[CT, G],
    rules: CFMMRules[G],
    logs: Logs[I, G]
  ): I[BoxHandler[F]] =
    logs.forService[CFMMOpsHandler[CT, F, G]].map(implicit log => new CFMMOpsHandler[CT, F, G].handler)
}
