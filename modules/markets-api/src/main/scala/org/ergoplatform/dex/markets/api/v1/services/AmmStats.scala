package org.ergoplatform.dex.markets.api.v1.services

import cats.Monad
import cats.data.OptionT
import org.ergoplatform.common.models.TimeWindow
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.dex.markets.api.v1.models.amm.{PlatformSummary, PoolSummary}
import org.ergoplatform.dex.markets.currencies.UsdUnits
import org.ergoplatform.dex.markets.domain.{Fees, TotalValueLocked, Volume}
import org.ergoplatform.dex.markets.modules.PriceSolver.{CryptoPriceSolver, FiatPriceSolver}
import org.ergoplatform.dex.markets.repositories.Pools

import scala.concurrent.duration.FiniteDuration
import tofu.syntax.monadic._

trait AmmStats[F[_]] {

  def getPlatformSummary(tail: FiniteDuration): F[PlatformSummary]

  def getPoolSummary(poolId: PoolId, window: TimeWindow): F[Option[PoolSummary]]
}

object AmmStats {

  final class Live[F[_]: Monad](pools: Pools[F], cryptoSolver: CryptoPriceSolver[F], fiatSolver: FiatPriceSolver[F])
    extends AmmStats[F] {

    def getPlatformSummary(tail: FiniteDuration): F[PlatformSummary] = ???

    def getPoolSummary(poolId: PoolId, window: TimeWindow): F[Option[PoolSummary]] =
      (for {
        pool    <- OptionT(pools.snapshot(poolId))
        lockedX <- OptionT(fiatSolver.convert(pool.lockedX, UsdUnits))
        lockedY <- OptionT(fiatSolver.convert(pool.lockedY, UsdUnits))
        tvl = TotalValueLocked(lockedX.value + lockedY.value, UsdUnits)

        vol  <- OptionT(pools.poolVolume(poolId, window))
        volX <- OptionT(fiatSolver.convert(vol.volumeByX, UsdUnits))
        volY <- OptionT(fiatSolver.convert(vol.volumeByY, UsdUnits))
        volume = Volume(volX.value + volY.value, UsdUnits, window)

        feesSnap <- OptionT(pools.poolFees(poolId, window))
        feesX    <- OptionT(fiatSolver.convert(feesSnap.feesByX, UsdUnits))
        feesY    <- OptionT(fiatSolver.convert(feesSnap.feesByY, UsdUnits))
        fees = Fees(feesX.value + feesY.value, UsdUnits, window)
      } yield PoolSummary(poolId, pool.lockedX, pool.lockedY, tvl, volume, fees)).value
  }
}
