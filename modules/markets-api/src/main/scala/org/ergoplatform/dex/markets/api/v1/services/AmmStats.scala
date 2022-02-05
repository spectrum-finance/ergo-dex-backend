package org.ergoplatform.dex.markets.api.v1.services

import cats.Monad
import cats.data.OptionT
import cats.effect.Clock
import mouse.anyf._
import org.ergoplatform.common.models.TimeWindow
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.dex.markets.api.v1.models.amm.{PlatformSummary, PoolStats, PoolSummary}
import org.ergoplatform.dex.markets.currencies.UsdUnits
import org.ergoplatform.dex.markets.domain.{Fees, TotalValueLocked, Volume}
import org.ergoplatform.dex.markets.modules.PriceSolver.FiatPriceSolver
import org.ergoplatform.dex.markets.repositories.Pools
import tofu.doobie.transactor.Txr
import mouse.anyf._
import cats.syntax.traverse._
import org.ergoplatform.dex.markets.db.models.amm.{PoolSnapshot, PoolTokenInfo, PoolVolumeSnapshot}
import org.ergoplatform.dex.markets.modules.AmmStatsMath
import tofu.syntax.monadic._
import tofu.syntax.time.now._

import scala.concurrent.duration._

trait AmmStats[F[_]] {

  def getPlatformSummary(window: TimeWindow): F[PlatformSummary]

  def getPoolSummary(poolId: PoolId, window: TimeWindow): F[Option[PoolSummary]]

  def getPoolsStats(window: TimeWindow): F[Map[String, PoolStats]]
}

object AmmStats {

  val MillisInYear: FiniteDuration = 365.days

  def make[F[_]: Monad: Clock, D[_]: Monad](implicit
    txr: Txr.Aux[F, D],
    pools: Pools[D],
    fiatSolver: FiatPriceSolver[F]
  ): AmmStats[F] = new Live[F, D]()

  final class Live[F[_]: Monad, D[_]: Monad](implicit
    txr: Txr.Aux[F, D],
    pools: Pools[D],
    fiatSolver: FiatPriceSolver[F],
    ammMath: AmmStatsMath[F]
  ) extends AmmStats[F] {

    def getPlatformSummary(window: TimeWindow): F[PlatformSummary] = {
      val queryPlatformStats =
        for {
          poolSnapshots <- pools.snapshots
          volumes       <- pools.volumes(window)
        } yield (poolSnapshots, volumes)
      for {
        (poolSnapshots, volumes) <- queryPlatformStats ||> txr.trans
        lockedX                  <- poolSnapshots.flatTraverse(pool => fiatSolver.convert(pool.lockedX, UsdUnits).map(_.toList))
        lockedY                  <- poolSnapshots.flatTraverse(pool => fiatSolver.convert(pool.lockedY, UsdUnits).map(_.toList))
        tvl = TotalValueLocked(lockedX.map(_.value).sum + lockedY.map(_.value).sum, UsdUnits)

        volumeByX <- volumes.flatTraverse(pool => fiatSolver.convert(pool.volumeByX, UsdUnits).map(_.toList))
        volumeByY <- volumes.flatTraverse(pool => fiatSolver.convert(pool.volumeByY, UsdUnits).map(_.toList))
        volume = Volume(volumeByX.map(_.value).sum + volumeByY.map(_.value).sum, UsdUnits, window)
      } yield PlatformSummary(tvl, volume)
    }

    def getPoolSummary(poolId: PoolId, window: TimeWindow): F[Option[PoolSummary]] = {
      val queryPoolStats =
        (for {
          info     <- OptionT(pools.info(poolId))
          pool     <- OptionT(pools.snapshot(poolId))
          vol      <- OptionT.liftF(pools.volume(poolId, window))
          feesSnap <- OptionT.liftF(pools.fees(poolId, window))
        } yield (info, pool, vol, feesSnap)).value
      (for {
        (info, pool, vol, feesSnap) <- OptionT(queryPoolStats ||> txr.trans)
        lockedX                     <- OptionT(fiatSolver.convert(pool.lockedX, UsdUnits))
        lockedY                     <- OptionT(fiatSolver.convert(pool.lockedY, UsdUnits))
        tvl = TotalValueLocked(lockedX.value + lockedY.value, UsdUnits)

        volume <- vol match {
                    case Some(vol) =>
                      for {
                        volX <- OptionT(fiatSolver.convert(vol.volumeByX, UsdUnits))
                        volY <- OptionT(fiatSolver.convert(vol.volumeByY, UsdUnits))
                      } yield Volume(volX.value + volY.value, UsdUnits, window)
                    case None => OptionT.pure[F](Volume.empty(UsdUnits, window))
                  }

        fees <- feesSnap match {
                  case Some(feesSnap) =>
                    for {
                      feesX <- OptionT(fiatSolver.convert(feesSnap.feesByX, UsdUnits))
                      feesY <- OptionT(fiatSolver.convert(feesSnap.feesByY, UsdUnits))
                    } yield Fees(feesX.value + feesY.value, UsdUnits, window)
                  case None => OptionT.pure[F](Fees.empty(UsdUnits, window))
                }

        yearlyFeesPercent <- OptionT.liftF(ammMath.feePercentProjection(tvl, fees, info, MillisInYear))
      } yield PoolSummary(poolId, pool.lockedX, pool.lockedY, tvl, volume, fees, yearlyFeesPercent)).value
    }

    def getPoolsStats(window: TimeWindow): F[Map[String, PoolStats]] = {
      val queryPoolStats = for {
        reports <- pools.tokenInfo
        volumes <- pools.volumes(window)
        snapshots <- pools.snapshots
      } yield (reports, volumes, snapshots)

      for {
        (tokensInfo: List[PoolTokenInfo], volumes: List[PoolVolumeSnapshot], snapshots: List[PoolSnapshot]) <- queryPoolStats ||> txr.trans

      } yield tokensInfo.flatMap { ti =>
        volumes.flatMap { vol =>
          snapshots.flatMap { snapshot =>
            Map(
              ti.baseId + "_" + ti.quoteId -> PoolStats(
                ti.baseId,
                ti.baseSymbol,
                ti.quoteId,
                ti.quoteSymbol,
                BigDecimal(snapshot.lockedX.amount) / snapshot.lockedY.amount,
                BigDecimal(vol.volumeByX.amount),
                BigDecimal(vol.volumeByY.amount)
              )
            )
          }
        }
      }.toMap
    }

  }
}
