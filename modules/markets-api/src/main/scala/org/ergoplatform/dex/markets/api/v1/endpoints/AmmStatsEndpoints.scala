package org.ergoplatform.dex.markets.api.v1.endpoints

import org.ergoplatform.common.http.HttpError
import org.ergoplatform.common.models.{HeightWindow, TimeWindow}
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.dex.markets.api.v1.models.amm.{PlatformSummary, PoolSlippage, PoolSummary, PricePoint}
import org.ergoplatform.dex.markets.api.v1.models.amm.{AmmMarketSummary, PlatformSummary, PoolSummary}
import org.ergoplatform.dex.markets.api.v1.models.locks.LiquidityLockInfo
import sttp.tapir.{path, Endpoint}
import sttp.tapir._
import sttp.tapir.json.circe.jsonBody

final class AmmStatsEndpoints {

  val PathPrefix = "amm"
  val Group      = "ammStats"

  def endpoints: List[Endpoint[_, _, _, _]] =
    getPoolLocks :: getPlatformStats :: getPoolStats :: getAvgPoolSlippage :: getPoolPriceChart :: getAmmMarkets :: Nil

  def getPoolLocks: Endpoint[(PoolId, Int), HttpError, List[LiquidityLockInfo], Any] =
    baseEndpoint.get
      .in(PathPrefix / "pool" / path[PoolId].description("Asset reference") / "locks")
      .in(query[Int]("leastDeadline").default(0).description("Least LQ Lock deadline"))
      .out(jsonBody[List[LiquidityLockInfo]])
      .tag(Group)
      .name("Pool locks")
      .description("Get liquidity locks for the pool with the given ID")

  def getPoolStats: Endpoint[(PoolId, TimeWindow), HttpError, PoolSummary, Any] =
    baseEndpoint.get
      .in(PathPrefix / "pool" / path[PoolId].description("Asset reference") / "stats")
      .in(timeWindow)
      .out(jsonBody[PoolSummary])
      .tag(Group)
      .name("Pool stats")
      .description("Get statistics on the pool with the given ID")

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
      .in(query[Int]("blockDepth").default(20).validate(Validator.min(1)).validate(Validator.max(10000)))
      .out(jsonBody[PoolSlippage])
      .tag(Group)
      .name("Pool slippage")
      .description("Get average slippage by pool")

  def getPoolPriceChart: Endpoint[(PoolId, HeightWindow, Int), HttpError, List[PricePoint], Any] =
    baseEndpoint.get
      .in(PathPrefix / "pool" / path[PoolId].description("Asset reference") / "chart")
      .in(heightWindow)
      .in(query[Int]("resolution").default(1).validate(Validator.min(1)))
      .out(jsonBody[List[PricePoint]])
      .tag(Group)
      .name("Pool chart")
      .description("Get price chart by pool")

  def getAmmMarkets: Endpoint[TimeWindow, HttpError, List[AmmMarketSummary], Any] =
    baseEndpoint.get
      .in(PathPrefix / "markets")
      .in(timeWindow)
      .out(jsonBody[List[AmmMarketSummary]])
      .tag(Group)
      .name("All pools stats")
      .description("Get statistics on all pools")
}
