package org.ergoplatform.dex.tracker.handlers

import cats.{Functor, FunctorFilter, Monad}
import mouse.any._
import org.ergoplatform.common.streaming.{Producer, Record}
import org.ergoplatform.ergo.state.{Confirmed, LedgerStatus}
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
  G[_]: Functor: Logging,
  Status[_]: LedgerStatus
](parsers: List[CFMMPoolsParser[CFMMType]])(implicit
  producer: Producer[PoolId, Status[CFMMPool], F]
) {

  def handler: BoxHandler[F] =
    _.map(o => parsers.map(_.pool(o)).reduce(_ orElse _)).unNone
      .evalTap(pool => info"CFMM pool update detected [$pool]")
      .map(pool => Record[PoolId, Status[CFMMPool]](pool.poolId, LedgerStatus.lift(pool)))
      .thrush(producer.produce)
}

object CFMMPoolsHandler {

  def make[
    I[_]: Functor,
    F[_]: Monad: Evals[*[_], G]: FunctorFilter,
    G[_]: Functor,
    Status[_]: LedgerStatus
  ](implicit
    producer: Producer[PoolId, Status[CFMMPool], F],
    logs: Logs[I, G]
  ): I[BoxHandler[F]] =
    logs.forService[CFMMPoolsHandler[F, G, Status]].map { implicit log =>
      val parsers =
        CFMMPoolsParser[T2T_CFMM] ::
        CFMMPoolsParser[N2T_CFMM] :: Nil
      new CFMMPoolsHandler[F, G, Status](parsers).handler
    }
}
