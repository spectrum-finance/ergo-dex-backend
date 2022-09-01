package org.ergoplatform.dex.markets.api.v1.services

import cats.data.OptionT
import cats.effect.Clock
import cats.syntax.option._
import cats.syntax.traverse._
import cats.syntax.parallel._
import cats.{Monad, Parallel}
import mouse.anyf._
import org.ergoplatform.common.models.TimeWindow
import org.ergoplatform.dex.domain.FullAsset
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.dex.markets.api.v1.models.amm._
import org.ergoplatform.dex.markets.api.v1.models.amm.types._
import org.ergoplatform.dex.markets.currencies.UsdUnits
import org.ergoplatform.dex.markets.db.models.amm._
import org.ergoplatform.dex.markets.domain.{CryptoVolume, Fees, TotalValueLocked, Volume}
import org.ergoplatform.dex.markets.db.models.amm
import org.ergoplatform.dex.markets.db.models.amm.{PoolFeesSnapshot, PoolSnapshot, PoolTrace, PoolVolumeSnapshot}
import org.ergoplatform.dex.markets.domain.{Fees, TotalValueLocked, Volume}
import org.ergoplatform.dex.markets.modules.AmmStatsMath
import org.ergoplatform.dex.markets.modules.PriceSolver.FiatPriceSolver
import org.ergoplatform.dex.markets.repositories.{Orders, Pools}
import org.ergoplatform.dex.markets.services.TokenFetcher
import org.ergoplatform.dex.protocol.constants.ErgoAssetId
import org.ergoplatform.dex.protocol.constants.ErgoAssetId
import org.ergoplatform.ergo.TokenId
import org.ergoplatform.ergo.modules.ErgoNetwork
import org.ergoplatform.ergo.{PubKey, TokenId}
import org.ergoplatform.{ErgoAddressEncoder, P2PKAddress}
import tofu.doobie.transactor.Txr
import tofu.syntax.monadic._

import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit
import tofu.syntax.time.now.millis
import scala.concurrent.duration._
import scala.math.BigDecimal.RoundingMode

trait AmmStats[F[_]] {

  def convertToFiat(id: TokenId, amount: Long): F[Option[FiatEquiv]]

  def getPlatformSummary(window: TimeWindow): F[PlatformSummary]

  def getPoolStats(poolId: PoolId, window: TimeWindow): F[Option[PoolStats]]

  def getPoolsStats(window: TimeWindow): F[List[PoolStats]]

  def getPoolsSummary: F[List[PoolSummary]]

  def getAvgPoolSlippage(poolId: PoolId, depth: Int): F[Option[PoolSlippage]]

  def getPoolPriceChart(poolId: PoolId, window: TimeWindow, resolution: Int): F[List[PricePoint]]

  def getSwapTransactions(window: TimeWindow): F[TransactionsInfo]

  def getDepositTransactions(window: TimeWindow): F[TransactionsInfo]

  def getLqProviderInfoWithOperations(address: String, pool: ApiPool, from: Long, to: Long): F[LqProviderAirdropInfo]

  def getSwapInfo(address: String): F[Option[TraderAirdropInfo]]

  def getLqProviderInfo(address: String): F[LpResultProd]

  def checkIfBetaTester(address: String): F[Option[BetaTesterInfo]]

  def getOffChainReward(
    address: String,
    from: Long,
    to: Option[Long],
    multiplier: Int,
    cohortSize: Option[Int]
  ): F[OffChainSpfReward]

  def getOffChainRewardsForAllAddresses(from: Long, to: Option[Long], multiplier: Int): F[List[OffChainCharts]]
}

object AmmStats {

  val MillisInYear: FiniteDuration = 365.days

  val slippageWindowScale = 2

  def make[F[_]: Monad: Parallel: Clock, D[_]: Monad](implicit
    txr: Txr.Aux[F, D],
    pools: Pools[D],
    orders: Orders[D],
    network: ErgoNetwork[F],
    tokens: TokenFetcher[F],
    fiatSolver: FiatPriceSolver[F],
    e: ErgoAddressEncoder
  ): AmmStats[F] = new Live[F, D]()

  final class Live[F[_]: Monad: Parallel, D[_]: Monad](implicit
    txr: Txr.Aux[F, D],
    pools: Pools[D],
    orders: Orders[D],
    tokens: TokenFetcher[F],
    network: ErgoNetwork[F],
    fiatSolver: FiatPriceSolver[F],
    ammMath: AmmStatsMath[F],
    e: ErgoAddressEncoder
  ) extends AmmStats[F] {

    def getOffChainRewardsForAllAddresses(from: Long, to: Option[Long], multiplier: Int): F[List[OffChainCharts]] = {
      def query: F[(List[String], Int)] = (for {
        addresses <- orders.getAllOffChainAddresses(from, to)
        count     <- orders.getOffChainParticipantsCount(from, to)
      } yield (addresses, count)) ||> txr.trans

      query.flatMap { case (addresses, count) =>
        addresses
          .map(getOffChainReward(_, from, to, multiplier, count.some))
          .sequence
          .map(_.map(reward => OffChainCharts(reward.address, reward.spfReward)).sortBy(_.spfReward).reverse)
      }
    }

    def getOffChainReward(
      address: String,
      from: Long,
      to: Option[Long],
      multiplier: Int,
      cohortSize: Option[Int]
    ): F[OffChainSpfReward] = {
      def query: F[(Option[OffChainOperatorState], Int, Int)] =
        (for {
          state     <- orders.getOffChainState(address, from, to)
          total     <- orders.getTotalOffChainOperationsCount(from, to)
          groupSize <- cohortSize.fold(orders.getOffChainParticipantsCount(from, to))(_.pure[D])
        } yield (state, total, groupSize)) ||> txr.trans

      query.map { case (stateOpt, total, size) =>
        stateOpt
          .map { state =>
            val spfReward = multiplier * size * (state.operations / BigDecimal(total))
            OffChainSpfReward(
              address,
              spfReward.setScale(6, RoundingMode.HALF_UP),
              state.operations,
              (state.totalReward / BigDecimal(10).pow(9)).setScale(9, RoundingMode.HALF_UP)
            )
          }
          .getOrElse(OffChainSpfReward.empty(address))
      }
    }

    def getSwapInfo(address: String): F[Option[TraderAirdropInfo]] =
      e
        .fromString(address)
        .collect { case address: P2PKAddress => address.pubkeyBytes }
        .map(PubKey.fromBytes)
        .toOption
        .map { pk: PubKey =>
          def query: F[(List[SwapState], List[PoolSnapshot], Int)] =
            (for {
              state <- orders.getSwapsState(pk)
              pools <- ApiPool.values.toList
                         .traverse(p => pools.snapshot(PoolId(TokenId.fromStringUnsafe(p.poolId))))
                         .map(_.flatten)
              users <- orders.getSwapUsersCount
            } yield (state, pools, users)) ||> txr.trans

          query.map { case (states, snapshots, groupSize) =>
            val userAmount = states.map { state =>
              if (state.inputId == ErgoAssetId.unwrapped) state.inputValue else state.outputAmount
            }.sum

            val poolAmount = snapshots.map { p1 =>
              if (p1.lockedY.id == ErgoAssetId) BigDecimal(p1.lockedY.amount) * 2
              else BigDecimal(p1.lockedX.amount) * 2
            }.sum

            val spfReward = (200 * groupSize * (userAmount / poolAmount)).setScale(6, RoundingMode.HALF_UP)

            TraderAirdropInfo(userAmount / BigDecimal(10).pow(9), spfReward, states.length)
          }
        }
        .sequence

    def getLqProviderInfoWithOperations(address: String, pool: ApiPool, from: Long, to: Long): F[LqProviderAirdropInfo] = {

      def query: F[(List[DBLpState], LqProviderStateDB, BigDecimal, Int)] =
        (for {
          states         <- orders.getLqProviderStates(address, pool.poolLp, from, to)
          state          <- orders.getLqProviderState(address, pool.poolLp)
          totalWeight    <- orders.getTotalWeight
          addressesCount <- orders.getLqUsers
        } yield (states, state.getOrElse(LqProviderStateDB.empty(address)), totalWeight, addressesCount)) ||> txr.trans

      query.map { case (states, state, totalWeight, addressesCount) =>
        val operations = states.sortBy(_.timestamp).map { state =>
          val df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
          LpState(
            pool.poolText,
            state.txId,
            state.balance,
            df.format(new Date(state.timestamp)),
            state.op,
            BigDecimal(state.amount)
          )
        }

        val spfResult = (2000 * addressesCount * (state.totalWeight / totalWeight)).setScale(6, RoundingMode.HALF_UP)

        LqProviderAirdropInfo(spfResult, state.totalWeight, operations)
      }
    }

    def checkIfBetaTester(address: String): F[Option[BetaTesterInfo]] = e
      .fromString(address)
      .collect { case address: P2PKAddress => address.pubkeyBytes }
      .map(PubKey.fromBytes)
      .toOption
      .map { pk: PubKey =>
        (orders.checkIfBetaTester(pk) ||> txr.trans)
          .map { res: Int =>
            if (res < 1) BetaTesterInfo(betaTester = false, 0) else BetaTesterInfo(betaTester = true, 500)
          }
      }
      .sequence

    def getLqProviderInfo(address: String): F[LpResultProd] =
      ApiPool.values.toList
        .map { apiPool =>
          def query: F[(LqProviderStateDB, BigDecimal, Int)] =
            (for {
              state          <- orders.getLqProviderState(address, apiPool.poolLp)
              totalWeight    <- orders.getTotalWeight
              addressesCount <- orders.getLqUsers
            } yield (state.getOrElse(LqProviderStateDB.empty(address)), totalWeight, addressesCount)) ||> txr.trans

          query.map { case (state, totalWeight, addressesCount) =>
            val weight = 2000 * addressesCount * (state.totalWeight / totalWeight)

            LpResultDev(
              weight.setScale(6, RoundingMode.HALF_UP),
              state.totalWeight,
              state.totalCount,
              state.totalErgValue,
              state.totalTime,
              s"${TimeUnit.MILLISECONDS.toHours(state.totalTime.toLong)}h",
              apiPool.poolText
            )
          }
        }
        .parSequence
        .map { pools =>
          LpResultProd(
            address,
            pools.map(_.totatSpfReward).sum,
            pools.map(_.totalWeight).sum,
            pools.map(_.totalErgValue).sum,
            s"${TimeUnit.MILLISECONDS.toHours(pools.map(_.totalTime).sum.toLong)}h",
            pools.map(_.operations).sum,
            pools
          )
        }

    def convertToFiat(id: TokenId, amount: Long): F[Option[FiatEquiv]] =
      (for {
        assetInfo <- OptionT(pools.assetById(id) ||> txr.trans)
        asset = FullAsset(
                  assetInfo.id,
                  amount * math.pow(10, assetInfo.evalDecimals).toLong,
                  assetInfo.ticker,
                  assetInfo.decimals
                )
        equiv <- OptionT(fiatSolver.convert(asset, UsdUnits, List.empty))
      } yield FiatEquiv(equiv.value, UsdUnits)).value

    def getPlatformSummary(window: TimeWindow): F[PlatformSummary] = {
      val queryPlatformStats =
        for {
          poolSnapshots <- pools.snapshots()
          volumes       <- pools.volumes(window)
        } yield (poolSnapshots, volumes)
      for {
        (poolSnapshots, volumes) <- queryPlatformStats ||> txr.trans
        validTokens              <- tokens.fetchTokens
        filteredSnaps =
          poolSnapshots.filter(ps => validTokens.contains(ps.lockedX.id) && validTokens.contains(ps.lockedY.id))
        lockedX <-
          filteredSnaps.flatTraverse(pool => fiatSolver.convert(pool.lockedX, UsdUnits, List.empty).map(_.toList))
        lockedY <-
          filteredSnaps.flatTraverse(pool => fiatSolver.convert(pool.lockedY, UsdUnits, List.empty).map(_.toList))
        tvl = TotalValueLocked(lockedX.map(_.value).sum + lockedY.map(_.value).sum, UsdUnits)

        volumeByX <-
          volumes.flatTraverse(pool => fiatSolver.convert(pool.volumeByX, UsdUnits, List.empty).map(_.toList))
        volumeByY <-
          volumes.flatTraverse(pool => fiatSolver.convert(pool.volumeByY, UsdUnits, List.empty).map(_.toList))
        volume = Volume(volumeByX.map(_.value).sum + volumeByY.map(_.value).sum, UsdUnits, window)
      } yield PlatformSummary(tvl, volume)
    }

    def getPoolsSummary: F[List[PoolSummary]] = {
      val day = 3600000 * 24

      val queryPoolData: TimeWindow => D[(List[PoolVolumeSnapshot], List[PoolSnapshot])] =
        tw =>
          for {
            volumes   <- pools.volumes(tw)
            snapshots <- pools.snapshots(true)
          } yield (volumes, snapshots)

      for {
        currTs <- millis
        tw = TimeWindow(Some(currTs - day), Some(currTs))
        validTokens                                                        <- tokens.fetchTokens
        (volumes: List[PoolVolumeSnapshot], snapshots: List[PoolSnapshot]) <- queryPoolData(tw) ||> txr.trans
        filtered = snapshots.filter(ps => ps.lockedX.id == ErgoAssetId && validTokens.contains(ps.lockedY.id))
        poolsTvl <- filtered.flatTraverse(p => processPoolTvl(p).map(_.map(tvl => (tvl, p))).map(_.toList))
        maxTvlPools =
          poolsTvl
            .groupBy { case (_, pool) => (pool.lockedX.id, pool.lockedY.id) }
            .map { case (_, tvls) =>
              tvls.maxBy(_._1.value)._2
            }
            .toList
        res = maxTvlPools.flatMap { pool =>
                volumes.find(_.poolId == pool.id).toList.map { vol =>
                  val x = pool.lockedX
                  val y = pool.lockedY
                  PoolSummary(
                    x.id,
                    x.ticker.get,
                    x.ticker.get,
                    y.id,
                    y.ticker.get,
                    y.ticker.get,
                    RealPrice
                      .calculate(x.amount, x.decimals, y.amount, y.decimals)
                      .setScale(6),
                    BigDecimal(vol.volumeByX.amount) / BigDecimal(10).pow(vol.volumeByX.decimals.getOrElse(0)),
                    BigDecimal(vol.volumeByY.amount) / BigDecimal(10).pow(vol.volumeByY.decimals.getOrElse(0))
                  )
                }
              }
      } yield res
    }

    private def processPoolTvl(pool: PoolSnapshot): F[Option[TotalValueLocked]] =
      (for {
        lockedX <- OptionT(fiatSolver.convert(pool.lockedX, UsdUnits, List.empty))
        lockedY <- OptionT(fiatSolver.convert(pool.lockedY, UsdUnits, List.empty))
        tvl = TotalValueLocked(lockedX.value + lockedY.value, UsdUnits)
      } yield tvl).value

    def getPoolsStats(window: TimeWindow): F[List[PoolStats]] =
      (pools.snapshots() ||> txr.trans).flatMap { snapshots: List[PoolSnapshot] =>
        snapshots
          .parTraverse(pool => getPoolSummaryUsingAllPools(pool, window, snapshots))
          .map(_.flatten)
      }

    private def getPoolSummaryUsingAllPools(
      pool: PoolSnapshot,
      window: TimeWindow,
      everyKnownPool: List[PoolSnapshot]
    ): F[Option[PoolStats]] = {
      val poolId = pool.id

      def poolData: D[Option[(amm.PoolInfo, Option[amm.PoolFeesSnapshot], Option[PoolVolumeSnapshot])]] =
        (for {
          info     <- OptionT(pools.info(poolId))
          feesSnap <- OptionT.liftF(pools.fees(poolId, window))
          vol      <- OptionT.liftF(pools.volume(poolId, window))
        } yield (info, feesSnap, vol)).value

      (for {
        (info, feesSnap, vol) <- OptionT(poolData ||> txr.trans)
        lockedX               <- OptionT(fiatSolver.convert(pool.lockedX, UsdUnits, everyKnownPool))
        lockedY               <- OptionT(fiatSolver.convert(pool.lockedY, UsdUnits, everyKnownPool))
        tvl = TotalValueLocked(lockedX.value + lockedY.value, UsdUnits)
        volume            <- processPoolVolume(vol, window, everyKnownPool)
        fees              <- processPoolFee(feesSnap, window, everyKnownPool)
        yearlyFeesPercent <- OptionT.liftF(ammMath.feePercentProjection(tvl, fees, info, MillisInYear))
      } yield PoolStats(poolId, pool.lockedX, pool.lockedY, tvl, volume, fees, yearlyFeesPercent)).value
    }

    private def processPoolVolume(
      vol: Option[PoolVolumeSnapshot],
      window: TimeWindow,
      everyKnownPool: List[PoolSnapshot]
    ): OptionT[F, Volume] =
      vol match {
        case Some(vol) =>
          for {
            volX <- OptionT(fiatSolver.convert(vol.volumeByX, UsdUnits, everyKnownPool))
            volY <- OptionT(fiatSolver.convert(vol.volumeByY, UsdUnits, everyKnownPool))
          } yield Volume(volX.value + volY.value, UsdUnits, window)
        case None => OptionT.pure[F](Volume.empty(UsdUnits, window))
      }

    private def processPoolFee(
      feesSnap: Option[PoolFeesSnapshot],
      window: TimeWindow,
      everyKnownPool: List[PoolSnapshot]
    ): OptionT[F, Fees] = feesSnap match {
      case Some(feesSnap) =>
        for {
          feesX <- OptionT(fiatSolver.convert(feesSnap.feesByX, UsdUnits, everyKnownPool))
          feesY <- OptionT(fiatSolver.convert(feesSnap.feesByY, UsdUnits, everyKnownPool))
        } yield Fees(feesX.value + feesY.value, UsdUnits, window)
      case None => OptionT.pure[F](Fees.empty(UsdUnits, window))
    }

    def getPoolStats(poolId: PoolId, window: TimeWindow): F[Option[PoolStats]] = {
      val queryPoolStats =
        (for {
          info     <- OptionT(pools.info(poolId))
          pool     <- OptionT(pools.snapshot(poolId))
          vol      <- OptionT.liftF(pools.volume(poolId, window))
          feesSnap <- OptionT.liftF(pools.fees(poolId, window))
        } yield (info, pool, vol, feesSnap)).value
      (for {
        (info, pool, vol, feesSnap) <- OptionT(queryPoolStats ||> txr.trans)
        lockedX                     <- OptionT(fiatSolver.convert(pool.lockedX, UsdUnits, List.empty))
        lockedY                     <- OptionT(fiatSolver.convert(pool.lockedY, UsdUnits, List.empty))
        tvl = TotalValueLocked(lockedX.value + lockedY.value, UsdUnits)
        volume            <- processPoolVolume(vol, window, List.empty)
        fees              <- processPoolFee(feesSnap, window, List.empty)
        yearlyFeesPercent <- OptionT.liftF(ammMath.feePercentProjection(tvl, fees, info, MillisInYear))
      } yield PoolStats(poolId, pool.lockedX, pool.lockedY, tvl, volume, fees, yearlyFeesPercent)).value
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

    def getSwapTransactions(window: TimeWindow): F[TransactionsInfo] =
      (for {
        swaps  <- OptionT.liftF(txr.trans(orders.getSwapTxs(window)))
        numTxs <- OptionT.fromOption[F](swaps.headOption.map(_.numTxs))
        volumes <- OptionT.liftF(
                     swaps.flatTraverse(swap =>
                       fiatSolver
                         .convert(swap.asset, UsdUnits, List.empty)
                         .map(_.toList.map(_.value))
                     )
                   )
      } yield TransactionsInfo(numTxs, volumes.sum / numTxs, volumes.max, UsdUnits).roundAvgValue)
        .getOrElse(TransactionsInfo.empty)

    def getDepositTransactions(window: TimeWindow): F[TransactionsInfo] =
      (for {
        deposits <- OptionT.liftF(orders.getDepositTxs(window) ||> txr.trans)
        numTxs   <- OptionT.fromOption[F](deposits.headOption.map(_.numTxs))
        volumes <- OptionT.liftF(deposits.flatTraverse { deposit =>
                     fiatSolver
                       .convert(deposit.assetX, UsdUnits, List.empty)
                       .flatMap { optX =>
                         fiatSolver
                           .convert(deposit.assetY, UsdUnits, List.empty)
                           .map(optY =>
                             optX
                               .flatMap(eqX => optY.map(eqY => eqX.value + eqY.value))
                               .toList
                           )
                       }
                   })
      } yield TransactionsInfo(numTxs, volumes.sum / numTxs, volumes.max, UsdUnits).roundAvgValue)
        .getOrElse(TransactionsInfo.empty)
  }
}
