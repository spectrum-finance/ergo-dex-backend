package org.ergoplatform.dex.markets.api.v1.endpoints

import io.circe.generic.auto._
import org.ergoplatform.common.http.HttpError
import org.ergoplatform.common.models.{Paging, TimeWindow}
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.dex.markets.api.v1.models.amm._
import org.ergoplatform.dex.markets.api.v1.models.locks.LiquidityLockInfo
import org.ergoplatform.dex.markets.configs.RequestConfig
import sttp.tapir.json.circe.jsonBody
import sttp.tapir._

final class AmmStatsEndpoints(conf: RequestConfig) {

  val PathPrefix = "amm"
  val Group      = "ammStats"

  def endpoints: List[Endpoint[_, _, _, _]] =
    getSwapTxs :: getDepositTxs :: getPoolLocks :: getPlatformStats ::
    getPoolStats :: getAvgPoolSlippage :: getPoolPriceChart ::
    convertToFiat :: getPoolsSummary :: Nil

  def getSwapTxs: Endpoint[TimeWindow, HttpError, TransactionsInfo, Any] =
    baseEndpoint.get
      .in(PathPrefix / "swaps")
      .in(timeWindow(conf.maxTimeWindow))
      .out(jsonBody[TransactionsInfo])
      .tag(Group)
      .name("Swap txs")
      .description("Get swap txs info")

  def getDepositTxs: Endpoint[TimeWindow, HttpError, TransactionsInfo, Any] =
    baseEndpoint.get
      .in(PathPrefix / "deposits")
      .in(timeWindow(conf.maxTimeWindow))
      .out(jsonBody[TransactionsInfo])
      .tag(Group)
      .name("Deposit txs")
      .description("Get deposit txs info")

  def getPoolLocks: Endpoint[(PoolId, Int), HttpError, List[LiquidityLockInfo], Any] =
    baseEndpoint.get
      .in(PathPrefix / "pool" / path[PoolId].description("Asset reference") / "locks")
      .in(query[Int]("leastDeadline").default(0).description("Least LQ Lock deadline"))
      .out(jsonBody[List[LiquidityLockInfo]])
      .tag(Group)
      .name("Pool locks")
      .description("Get liquidity locks for the pool with the given ID")

  def getPoolStats: Endpoint[(PoolId, TimeWindow), HttpError, PoolStats, Any] =
    baseEndpoint.get
      .in(PathPrefix / "pool" / path[PoolId].description("Asset reference") / "stats")
      .in(timeWindow)
      .out(jsonBody[PoolStats])
      .tag(Group)
      .name("Pool stats")
      .description("Get statistics on the pool with the given ID")

  def getPoolsStats: Endpoint[TimeWindow, HttpError, List[PoolStats], Any] =
    baseEndpoint.get
      .in(PathPrefix / "pools" / "stats")
      .in(timeWindow)
      .out(jsonBody[List[PoolStats]])
      .tag(Group)
      .name("Pools statistic")
      .description("Get statistic about every known pool")

  def getPoolsSummary: Endpoint[Unit, HttpError, List[PoolSummary], Any] =
    baseEndpoint.get
      .in(PathPrefix / "pools" / "summary")
      .out(jsonBody[List[PoolSummary]])
      .tag(Group)
      .name("Pools summary")
      .description("Get summary by every known pool with max TVL")

  def getPlatformStats: Endpoint[TimeWindow, HttpError, PlatformSummary, Any] =
    baseEndpoint.get
      .in(PathPrefix / "platform" / "stats")
      .in(timeWindow)
      .out(jsonBody[PlatformSummary])
      .tag(Group)
      .name("Platform stats")
      .description("Get statistics on whole AMM")

  def getAvgPoolSlippage: Endpoint[(PoolId, Int), HttpError, PoolSlippage, Any] =
    baseEndpoint.get
      .in(PathPrefix / "pool" / path[PoolId].description("Asset reference") / "slippage")
      .in(query[Int]("blockDepth").default(20).validate(Validator.min(1)).validate(Validator.max(128)))
      .out(jsonBody[PoolSlippage])
      .tag(Group)
      .name("Pool slippage")
      .description("Get average slippage by pool")

  def getPoolPriceChart: Endpoint[(PoolId, TimeWindow, Int), HttpError, List[PricePoint], Any] =
    baseEndpoint.get
      .in(PathPrefix / "pool" / path[PoolId].description("Asset reference") / "chart")
      .in(timeWindow)
      .in(query[Int]("resolution").default(1).validate(Validator.min(1)))
      .out(jsonBody[List[PricePoint]])
      .tag(Group)
      .name("Pool chart")
      .description("Get price chart by pool")

  def convertToFiat: Endpoint[ConvertionRequest, HttpError, FiatEquiv, Any] =
    baseEndpoint.post
      .in(PathPrefix / "convert")
      .in(jsonBody[ConvertionRequest])
      .out(jsonBody[FiatEquiv])
      .tag(Group)
      .name("Crypto/Fiat conversion")
      .description("Convert crypto units to fiat")

  def getUsersOrderHistory: Endpoint[(Paging, TimeWindow, OrdersRequest), HttpError, List[Order], Any] =
    baseEndpoint.post
      .in(PathPrefix / "orders")
      .in(paging)
      .in(timeWindow)
      .in(jsonBody[OrdersRequest])
      .out(jsonBody[List[Order]])
      .tag(Group)
      .name("Users orders")
      .description("Get users order history")
}
