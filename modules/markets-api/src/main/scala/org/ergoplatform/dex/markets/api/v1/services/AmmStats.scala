package org.ergoplatform.dex.markets.api.v1.services

import cats.Monad
import cats.data.OptionT
import cats.effect.Clock
import mouse.anyf._
import org.ergoplatform.common.models.{HeightWindow, TimeWindow}
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.dex.markets.api.v1.models.amm.{AmmMarketSummary, PlatformSummary, PoolSummary}
import org.ergoplatform.dex.markets.api.v1.models.amm.types._
import org.ergoplatform.dex.markets.api.v1.models.amm.{PlatformSummary, PoolSlippage, PoolSummary, PricePoint}
import org.ergoplatform.dex.markets.currencies.UsdUnits
import org.ergoplatform.dex.markets.domain.{CryptoVolume, Fees, TotalValueLocked, Volume}
import org.ergoplatform.dex.markets.modules.PriceSolver.FiatPriceSolver
import org.ergoplatform.dex.markets.repositories.Pools
import tofu.doobie.transactor.Txr
import mouse.anyf._
import cats.syntax.traverse._
import org.ergoplatform.dex.markets.db.models.amm.{
  AvgAssetAmounts,
  PoolInfo,
  PoolSnapshot,
  PoolTrace,
  PoolVolumeSnapshot
}
import org.ergoplatform.dex.domain.{AssetClass, CryptoUnits, MarketId}
import org.ergoplatform.dex.markets.modules.AmmStatsMath
import org.ergoplatform.ergo.modules.ErgoNetwork
import tofu.syntax.monadic._
import tofu.syntax.time.now._

import scala.collection.immutable
import scala.concurrent.duration._
import scala.math.BigDecimal.RoundingMode

trait AmmStats[F[_]] {

  def getPlatformSummary(window: TimeWindow): F[PlatformSummary]

  def getPoolSummary(poolId: PoolId, window: TimeWindow): F[Option[PoolSummary]]

  def getAvgPoolSlippage(poolId: PoolId, depth: Int): F[Option[PoolSlippage]]

  def getPoolPriceChart(poolId: PoolId, window: HeightWindow, resolution: Int): F[List[PricePoint]]

  def getMarkets(window: TimeWindow): F[List[AmmMarketSummary]]
}

object AmmStats {

  val MillisInYear: FiniteDuration = 365.days

  def make[F[_]: Monad: Clock, D[_]: Monad](implicit
    txr: Txr.Aux[F, D],
    pools: Pools[D],
    network: ErgoNetwork[F],
    fiatSolver: FiatPriceSolver[F]
  ): AmmStats[F] = new Live[F, D]()

  final class Live[F[_]: Monad, D[_]: Monad](implicit
    txr: Txr.Aux[F, D],
    pools: Pools[D],
    network: ErgoNetwork[F],
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

    def getAvgPoolSlippage(poolId: PoolId, depth: Int): F[Option[PoolSlippage]] =
      network.getCurrentHeight.flatMap { currHeight =>

        val query = for {
          traces  <- pools.trace(poolId, depth, currHeight)
          poolOpt <- pools.info(poolId)
        } yield (traces, poolOpt)

        val transed: F[(List[PoolTrace], Option[PoolInfo])] = txr.trans(query)

        transed.map { case (traces, poolOpt) =>
          poolOpt.fold[Option[PoolSlippage]](None) { _ =>
            traces match {
              case Nil => Some(PoolSlippage(0))
              case xs: List[PoolTrace] =>
                val slippageBySegment = xs
                  .groupBy(_.height)
                  .map { case (_, traces) =>
                    val max = traces.maxBy(_.gindex)
                    val min = traces.minBy(_.gindex)
                    val minPrice = RealPrice.calculate(
                      min.lockedX.amount,
                      min.lockedX.decimals,
                      min.lockedY.amount,
                      min.lockedY.decimals
                    )
                    val maxPrice = RealPrice.calculate(
                      max.lockedX.amount,
                      max.lockedX.decimals,
                      max.lockedY.amount,
                      max.lockedY.decimals
                    )
                    (maxPrice.value - minPrice.value).abs / (minPrice.value / 100)
                  }
                  .toList
                Some(PoolSlippage(slippageBySegment.sum / slippageBySegment.size).scale(3))
            }
          }
        }
      }

    def getPoolPriceChart(poolId: PoolId, window: HeightWindow, resolution: Int): F[List[PricePoint]] = {

      val queryPoolData = for {
        amounts   <- pools.avgAmounts(poolId, window, resolution)
        snapshots <- pools.snapshot(poolId)
      } yield (amounts, snapshots)

      txr
        .trans(queryPoolData)
        .map { case (amounts, snapOpt) =>
          snapOpt.fold(List.empty[PricePoint]) { snap =>
            amounts.map { amount =>
              val price =
                RealPrice.calculate(amount.amountX, snap.lockedX.decimals, amount.amountY, snap.lockedY.decimals)
              PricePoint(amount.index * resolution, price.setScale(6))
            }
          }
        }
    }

    def getMarkets(window: TimeWindow): F[List[AmmMarketSummary]] = {
      val queryPoolStats = for {
        volumes   <- pools.volumes(window)
        snapshots <- pools.snapshots
      } yield (volumes, snapshots)

      txr
        .trans(queryPoolStats)
        .map { case (volumes: List[PoolVolumeSnapshot], snapshots: List[PoolSnapshot]) =>
          snapshots.flatMap { snapshot =>
            val currentOpt = volumes
              .find(_.poolId == snapshot.id)

            currentOpt.toList.map { vol =>
              val tx = snapshot.lockedX
              val ty = snapshot.lockedY
              val vx = vol.volumeByX
              val vy = vol.volumeByY
              AmmMarketSummary(
                MarketId(tx.id, ty.id),
                tx.id,
                tx.ticker,
                ty.id,
                ty.ticker,
                RealPrice.calculate(tx.amount, tx.decimals, ty.amount, ty.decimals).setScale(6),
                CryptoVolume(
                  BigDecimal(vx.amount),
                  CryptoUnits(AssetClass(vx.id, vx.ticker, vx.decimals)),
                  window
                ),
                CryptoVolume(
                  BigDecimal(vy.amount),
                  CryptoUnits(AssetClass(vy.id, vy.ticker, vy.decimals)),
                  window
                )
              )
            }
          }
        }
    }
  }
}
