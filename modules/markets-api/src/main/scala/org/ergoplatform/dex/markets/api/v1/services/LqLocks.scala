package org.ergoplatform.dex.markets.api.v1.services

import cats.Functor
import mouse.anyf._
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.dex.markets.api.v1.models.locks.LiquidityLockInfo
import org.ergoplatform.dex.markets.repositories.Locks
import tofu.doobie.transactor.Txr
import tofu.syntax.monadic._

trait LqLocks[F[_]] {

  def byPool(poolId: PoolId, leastDeadline: Int): F[List[LiquidityLockInfo]]
}

object LqLocks {

  def make[F[_], D[_]: Functor](implicit txr: Txr.Aux[F, D], locks: Locks[D]): LqLocks[F] =
    new Live()

  final class Live[F[_], D[_]: Functor](implicit txr: Txr.Aux[F, D], locks: Locks[D]) extends LqLocks[F] {

    def byPool(poolId: PoolId, leastDeadline: Int): F[List[LiquidityLockInfo]] =
      locks.byPool(poolId, leastDeadline).map(_.map(LiquidityLockInfo(_))) ||> txr.trans
  }
}
