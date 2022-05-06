package org.ergoplatform.dex.markets.api.v1.services

import cats.Monad
import cats.data.OptionT
import cats.effect.Clock
import mouse.anyf._
import org.ergoplatform.common.models.TimeWindow
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.dex.markets.api.v1.models.amm.{AmmMarketSummary, FiatEquiv, PlatformSummary, PoolSummary}
import org.ergoplatform.dex.markets.api.v1.models.amm.types._
import org.ergoplatform.dex.markets.api.v1.models.amm.{PoolSlippage, PricePoint}
import org.ergoplatform.dex.markets.currencies.UsdUnits
import org.ergoplatform.dex.markets.domain.{CryptoVolume, Fees, TotalValueLocked, Volume}
import org.ergoplatform.dex.markets.modules.PriceSolver.FiatPriceSolver
import org.ergoplatform.dex.markets.repositories.Pools
import tofu.doobie.transactor.Txr
import mouse.anyf._
import cats.syntax.traverse._
import org.ergoplatform.dex.markets.db.models.amm.{
  PoolFeesSnapshot,
  PoolInfo,
  PoolSnapshot,
  PoolTrace,
  PoolVolumeSnapshot
}
import org.ergoplatform.dex.domain.{AssetClass, AssetEquiv, CryptoUnits, FullAsset, MarketId}
import org.ergoplatform.dex.markets.modules.AmmStatsMath
import org.ergoplatform.ergo.TokenId
import org.ergoplatform.ergo.modules.ErgoNetwork
import tofu.syntax.monadic._
import tofu.syntax.time.now._
import scala.concurrent.duration._

trait AmmStats[F[_]] {

  def convertToFiat(id: TokenId, amount: Long): F[Option[FiatEquiv]]

  def getPlatformSummary(window: TimeWindow): F[PlatformSummary]

  def getPoolSummary(poolId: PoolId, window: TimeWindow): F[Option[PoolSummary]]

  def getPoolsSummary(window: TimeWindow): F[List[PoolSummary]]

  def getAvgPoolSlippage(poolId: PoolId, depth: Int): F[Option[PoolSlippage]]

  def getPoolPriceChart(poolId: PoolId, window: TimeWindow, resolution: Int): F[List[PricePoint]]

  def getMarkets(window: TimeWindow): F[List[AmmMarketSummary]]
}

object AmmStats {

  val MillisInYear: FiniteDuration = 365.days

  val slippageWindowScale = 2

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

    def convertToFiat(id: TokenId, amount: Long): F[Option[FiatEquiv]] =
      (for {
        assetInfo <- OptionT(pools.assetById(id) ||> txr.trans)
        asset = FullAsset(
                  assetInfo.id,
                  amount * math.pow(10, assetInfo.evalDecimals).toLong,
                  assetInfo.ticker,
                  assetInfo.decimals
                )
        equiv <- OptionT(fiatSolver.convert(asset, UsdUnits))
      } yield FiatEquiv(equiv.value, UsdUnits)).value

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

    private def calculateTVLsFor(pools: List[PoolSnapshot]): F[List[TotalValueLocked]] =
      pools
        .traverse { pool =>
          fiatSolver
            .convert(pool.lockedX, UsdUnits)
            .flatMap(optX => fiatSolver.convert(pool.lockedY, UsdUnits).map(optY => (pool, optX, optY)))
        }
        .map { equivOpts =>
          val equivs = equivOpts.filter { case (_, eqX, eqY) => eqX.isDefined && eqY.isDefined }
          equivs.map { case (_, lx, ly) => TotalValueLocked(lx.get.value + ly.get.value, UsdUnits) }
        }

    private def convertVolumes(pools: List[PoolSnapshot], volumes: List[PoolVolumeSnapshot], window: TimeWindow) =
      pools.map(p => volumes.find(_.poolId == p.id)).traverse {
        case Some(vol) =>
          for {
            volX <- fiatSolver.convert(vol.volumeByX, UsdUnits)
            volY <- fiatSolver.convert(vol.volumeByY, UsdUnits)
          } yield
            if (volX.isDefined && volY.isDefined) Volume(volX.get.value + volY.get.value, UsdUnits, window)
            else Volume.empty(UsdUnits, window)
        case None => Volume.empty(UsdUnits, window).pure[F]
      }

    private def convertFees(pools: List[PoolSnapshot], fees: List[PoolFeesSnapshot], window: TimeWindow) =
      pools.map(p => fees.find(_.poolId == p.id)).traverse {
        case Some(feesSnap) =>
          for {
            feesX <- fiatSolver.convert(feesSnap.feesByX, UsdUnits)
            feesY <- fiatSolver.convert(feesSnap.feesByY, UsdUnits)
          } yield
            if (feesX.isDefined && feesY.isDefined) Fees(feesX.get.value + feesY.get.value, UsdUnits, window)
            else Fees.empty(UsdUnits, window)
        case None => Fees.empty(UsdUnits, window).pure[F]
      }

    def getPoolsSummary(window: TimeWindow): F[List[PoolSummary]] = {
      val queryPoolsStats =
        for {
          snaps  <- pools.snapshots
          tupled <- snaps.traverse(p => pools.info(p.id).map(opt => (p, opt)))
          (allPools, info) = tupled.filter(_._2.isDefined).unzip
          volumes <- pools.volumes(window)
          fees    <- pools.fees(window)
        } yield (allPools, info, volumes, fees)
      for {
        (
          pools: List[PoolSnapshot],
          info: List[Option[PoolInfo]],
          volumes: List[PoolVolumeSnapshot],
          fees: List[PoolFeesSnapshot]
        )        <- queryPoolsStats ||> txr.trans
        tvls     <- calculateTVLsFor(pools)
        vols     <- convertVolumes(pools, volumes, window)
        feeSnaps <- convertFees(pools, fees, window)
        fullPoolInfos = (tvls, vols, feeSnaps).zipped.toList zip info zip pools
        res <- fullPoolInfos.traverse { case (((tvl, vol, fee), inf), pool) =>
                 ammMath
                   .feePercentProjection(tvl, fee, inf.get, MillisInYear)
                   .map(perc => PoolSummary(pool.id, pool.lockedX, pool.lockedY, tvl, vol, fee, perc))
               }
      } yield res
    }

    //        vols <- allPools.map(p => volumes.find(_.poolId == p.id)).foldLeft(List.empty[Volume].pure[F]) { case (acc, a) =>
    //          acc.flatMap( li =>
    //          a match {
    //            case Some(vol) =>
    //              for {
    //                volX <- fiatSolver.convert(vol.volumeByX, UsdUnits)
    //                volY <- fiatSolver.convert(vol.volumeByY, UsdUnits)
    //               } yield {
    //                if (volX.isDefined && volY.isDefined) li :+ Volume(volX.get.value + volY.get.value, UsdUnits, window)
    //                else li
    //            }
    //            case None => (li :+ Volume.empty(UsdUnits, window)).pure[F]
    //          }
    //          )
    //        }

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

    private def calculatePoolSlippagePercent(initState: PoolTrace, finalState: PoolTrace): BigDecimal = {
      val minPrice = RealPrice.calculate(
        initState.lockedX.amount,
        initState.lockedX.decimals,
        initState.lockedY.amount,
        initState.lockedY.decimals
      )
      val maxPrice = RealPrice.calculate(
        finalState.lockedX.amount,
        finalState.lockedX.decimals,
        finalState.lockedY.amount,
        finalState.lockedY.decimals
      )
      (maxPrice.value - minPrice.value).abs / (minPrice.value / 100)
    }

    def getAvgPoolSlippage(poolId: PoolId, depth: Int): F[Option[PoolSlippage]] =
      network.getCurrentHeight.flatMap { currHeight =>
        val query = for {
          initialState <- pools.prevTrace(poolId, depth, currHeight)
          traces       <- pools.trace(poolId, depth, currHeight)
          poolOpt      <- pools.info(poolId)
        } yield (traces, initialState, poolOpt)

        txr.trans(query).map { case (traces, initStateOpt, poolOpt) =>
          poolOpt.flatMap { _ =>
            traces match {
              case Nil => Some(PoolSlippage.zero)
              case xs =>
                val groupedTraces = xs
                  .sortBy(_.gindex)
                  .groupBy(_.height / slippageWindowScale)
                  .toList
                  .sortBy(_._1)
                val initState                  = initStateOpt.getOrElse(xs.minBy(_.gindex))
                val maxState                   = groupedTraces.head._2.maxBy(_.gindex)
                val firstWindowSlippagePercent = calculatePoolSlippagePercent(initState, maxState)

                groupedTraces.drop(1) match {
                  case Nil => Some(PoolSlippage(firstWindowSlippagePercent).scale(PoolSlippage.defaultScale))
                  case restTraces =>
                    val restWindowsSlippage = restTraces
                      .map { case (_, heightWindow) =>
                        val windowMinGindex = heightWindow.minBy(_.gindex).gindex
                        val min = xs.filter(_.gindex < windowMinGindex) match {
                          case Nil      => heightWindow.minBy(_.gindex)
                          case filtered => filtered.maxBy(_.gindex)
                        }
                        val max = heightWindow.maxBy(_.gindex)
                        calculatePoolSlippagePercent(min, max)
                      }
                    val slippageBySegment = firstWindowSlippagePercent +: restWindowsSlippage
                    Some(PoolSlippage(slippageBySegment.sum / slippageBySegment.size).scale(PoolSlippage.defaultScale))
                }
            }
          }
        }
      }

    def getPoolPriceChart(poolId: PoolId, window: TimeWindow, resolution: Int): F[List[PricePoint]] = {

      val queryPoolData = for {
        amounts   <- pools.avgAmounts(poolId, window, resolution)
        snapshots <- pools.snapshot(poolId)
      } yield (amounts, snapshots)

      txr
        .trans(queryPoolData)
        .map {
          case (amounts, Some(snap)) =>
            amounts.map { amount =>
              val price =
                RealPrice.calculate(amount.amountX, snap.lockedX.decimals, amount.amountY, snap.lockedY.decimals)
              PricePoint(amount.timestamp, price.setScale(RealPrice.defaultScale))
            }
          case _ => List.empty
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
