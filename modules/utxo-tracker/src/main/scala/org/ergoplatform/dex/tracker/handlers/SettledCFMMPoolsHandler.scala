package org.ergoplatform.dex.tracker.handlers

import cats.{Functor, FunctorFilter, Monad}
import mouse.any._
import org.ergoplatform.common.streaming.{Producer, Record}
import org.ergoplatform.dex.domain.amm.{CFMMPool, PoolId}
import org.ergoplatform.dex.protocol.amm.AMMType.{CFMMType, N2T_CFMM, T2T_CFMM}
import org.ergoplatform.dex.tracker.parsers.amm.CFMMPoolsParser
import org.ergoplatform.ergo.domain.LedgerMetadata
import org.ergoplatform.ergo.state.ConfirmedIndexed
import tofu.logging.{Logging, Logs}
import tofu.streams.Evals
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.streams.all._

final class SettledCFMMPoolsHandler[
  F[_]: Monad: Evals[*[_], G]: FunctorFilter,
  G[_]: Functor: Logging
](parsers: List[CFMMPoolsParser[CFMMType]])(implicit
  producer: Producer[PoolId, ConfirmedIndexed[CFMMPool], F]
) {

  def handler: SettledBoxHandler[F] =
    _.map(o => parsers.map(_.pool(o.output).map(p => ConfirmedIndexed(p, LedgerMetadata(o)))).reduce(_ orElse _)).unNone
      .evalTap(pool => info"CFMM pool update detected [$pool]")
      .map(pool => Record[PoolId, ConfirmedIndexed[CFMMPool]](pool.entity.poolId, pool))
      .thrush(producer.produce)
}

object SettledCFMMPoolsHandler {

  def make[
    I[_]: Functor,
    F[_]: Monad: Evals[*[_], G]: FunctorFilter,
    G[_]: Functor
  ](implicit
    producer: Producer[PoolId, ConfirmedIndexed[CFMMPool], F],
    logs: Logs[I, G]
  ): I[SettledBoxHandler[F]] =
    logs.forService[SettledCFMMPoolsHandler[F, G]].map { implicit log =>
      val parsers =
        CFMMPoolsParser[T2T_CFMM] ::
        CFMMPoolsParser[N2T_CFMM] :: Nil
      new SettledCFMMPoolsHandler[F, G](parsers).handler
    }
}
