package org.ergoplatform.dex.markets.api.v1.services

import cats.Functor
import mouse.anyf._
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.dex.markets.api.v1.models.locks.LiquidityLockInfo
import org.ergoplatform.dex.markets.repositories.Locks
import tofu.doobie.transactor.Txr
import tofu.syntax.monadic._

trait LqLockStats[F[_]] {

  def byPool(poolId: PoolId): F[List[LiquidityLockInfo]]
}

object LqLockStats {

  def make[F[_], D[_]: Functor](implicit txr: Txr.Aux[F, D], locks: Locks[D]): LqLockStats[F] =
    new Live()

  final class Live[F[_], D[_]: Functor](implicit txr: Txr.Aux[F, D], locks: Locks[D]) extends LqLockStats[F] {

    def byPool(poolId: PoolId): F[List[LiquidityLockInfo]] =
      locks.byPool(poolId).map(_.map(LiquidityLockInfo(_))) ||> txr.trans
  }
}
