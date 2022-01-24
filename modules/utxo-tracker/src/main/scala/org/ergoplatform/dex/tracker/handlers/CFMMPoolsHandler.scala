package org.ergoplatform.dex.tracker.handlers

import cats.{Functor, FunctorFilter, Monad}
import mouse.any._
import org.ergoplatform.common.streaming.{Producer, Record}
import org.ergoplatform.dex.domain.amm.state.Confirmed
import org.ergoplatform.dex.domain.amm.{CFMMPool, PoolId}
import org.ergoplatform.dex.protocol.amm.AMMType.{CFMMType, N2T_CFMM, T2T_CFMM}
import org.ergoplatform.dex.tracker.parsers.amm.CFMMPoolsParser
import tofu.logging.{Logging, Logs}
import tofu.streams.Evals
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.streams.all._

final class CFMMPoolsHandler[
  F[_]: Monad: Evals[*[_], G]: FunctorFilter,
  G[_]: Functor: Logging
](parsers: List[CFMMPoolsParser[CFMMType]])(implicit
  producer: Producer[PoolId, Confirmed[CFMMPool], F]
) {

  def handler: BoxHandler[F] =
    _.map(o => parsers.map(_.pool(o)).reduce(_ orElse _)).unNone
      .evalTap(pool => info"CFMM pool update detected [$pool]")
      .map(pool => Record[PoolId, Confirmed[CFMMPool]](pool.confirmed.poolId, pool))
      .thrush(producer.produce)
}

object CFMMPoolsHandler {

  def make[
    I[_]: Functor,
    F[_]: Monad: Evals[*[_], G]: FunctorFilter,
    G[_]: Functor
  ](implicit
    producer: Producer[PoolId, Confirmed[CFMMPool], F],
    logs: Logs[I, G]
  ): I[BoxHandler[F]] =
    logs.forService[CFMMPoolsHandler[F, G]].map { implicit log =>
      val parsers =
        CFMMPoolsParser[T2T_CFMM] ::
        CFMMPoolsParser[N2T_CFMM] :: Nil
      new CFMMPoolsHandler[F, G](parsers).handler
    }
}
