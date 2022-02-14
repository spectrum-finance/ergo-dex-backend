package org.ergoplatform.dex.tracker.handlers

import cats.{Functor, FunctorFilter, Monad}
import org.ergoplatform.common.streaming.{Producer, Record}
import org.ergoplatform.ergo.state.Confirmed
import org.ergoplatform.dex.domain.locks.LiquidityLock
import org.ergoplatform.dex.domain.locks.types.LockId
import org.ergoplatform.dex.protocol.ProtoVer
import org.ergoplatform.dex.tracker.parsers.locks.LiquidityLockParser
import tofu.logging.{Logging, Logs}
import tofu.streams.Evals
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.streams.all._
import mouse.any._
import org.ergoplatform.ErgoAddressEncoder

final class LiquidityLocksHandler[
  F[_]: Monad: Evals[*[_], G]: FunctorFilter,
  G[_]: Functor: Logging
](parsers: List[LiquidityLockParser[_]])(implicit
  producer: Producer[LockId, Confirmed[LiquidityLock], F]
) {

  def handler: BoxHandler[F] =
    _.map(o => parsers.map(_.parse(o)).reduce(_ orElse _)).unNone
      .evalTap(lock => info"LQ lock detected [$lock]")
      .map(lock => Record[LockId, Confirmed[LiquidityLock]](lock.entity.id, lock))
      .thrush(producer.produce)
}

object LiquidityLocksHandler {

  def make[
    I[_]: Functor,
    F[_]: Monad: Evals[*[_], G]: FunctorFilter,
    G[_]: Functor
  ](implicit
    producer: Producer[LockId, Confirmed[LiquidityLock], F],
    e: ErgoAddressEncoder,
    logs: Logs[I, G]
  ): I[BoxHandler[F]] =
    logs.forService[LiquidityLocksHandler[F, G]].map { implicit log =>
      val parsers = implicitly[LiquidityLockParser[ProtoVer.V0]] :: Nil
      new LiquidityLocksHandler[F, G](parsers).handler
    }
}
