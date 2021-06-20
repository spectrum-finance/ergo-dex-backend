package org.ergoplatform.dex.tracker.handlers

import cats.{Functor, FunctorFilter, Monad}
import mouse.any._
import org.ergoplatform.common.streaming.{Producer, Record}
import org.ergoplatform.dex.domain.amm.{CFMMPool, PoolId}
import org.ergoplatform.dex.protocol.amm.AMMType.CFMMFamily
import org.ergoplatform.dex.tracker.parsers.amm.CFMMPoolsParser
import tofu.logging.{Logging, Logs}
import tofu.streams.Evals
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.streams.all._

final class CFMMPoolsHandler[
  CF <: CFMMFamily,
  F[_]: Monad: Evals[*[_], G]: FunctorFilter,
  G[_]: Functor: Logging
](implicit producer: Producer[PoolId, CFMMPool, F], parser: CFMMPoolsParser[CF]) {

  def handler: BoxHandler[F] =
    _.map(parser.pool).unNone
      .evalTap(pool => info"CFMM pool update detected [$pool]")
      .map(op => Record[PoolId, CFMMPool](op.onChain.poolId, op.onChain))
      .thrush(producer.produce)
}

object CFMMPoolsHandler {

  def make[
    CF <: CFMMFamily,
    I[_]: Functor,
    F[_]: Monad: Evals[*[_], G]: FunctorFilter,
    G[_]: Functor
  ](implicit
    producer: Producer[PoolId, CFMMPool, F],
    parser: CFMMPoolsParser[CF],
    logs: Logs[I, G]
  ): I[BoxHandler[F]] =
    logs.forService[CFMMPoolsHandler[CF, F, G]].map(implicit log => new CFMMPoolsHandler[CF, F, G].handler)
}
