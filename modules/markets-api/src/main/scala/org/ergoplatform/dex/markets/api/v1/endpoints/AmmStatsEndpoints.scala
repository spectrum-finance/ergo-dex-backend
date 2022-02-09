package org.ergoplatform.dex.markets.api.v1.endpoints

import org.ergoplatform.common.http.HttpError
import org.ergoplatform.common.models.TimeWindow
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.dex.markets.api.v1.models.amm.{PlatformSummary, PoolSummary}
import org.ergoplatform.dex.markets.api.v1.models.locks.LiquidityLockInfo
import sttp.tapir.{Endpoint, path}
import sttp.tapir._
import sttp.tapir.json.circe.jsonBody

final class AmmStatsEndpoints {

  val PathPrefix = "amm"
  val Group      = "ammStats"

  def endpoints: List[Endpoint[_, _, _, _]] = getPoolLocks :: getPlatformStats :: getPoolStats :: Nil

  def getPoolLocks: Endpoint[(PoolId, Int), HttpError, List[LiquidityLockInfo], Any] =
    baseEndpoint.get
      .in(PathPrefix / "pool" / path[PoolId].description("Asset reference") / "locks")
      .in(query[Int]("leastDeadline").default(0).description("Least LQ Lock deadline"))
      .out(jsonBody[List[LiquidityLockInfo]])
      .tag(Group)
      .name("Pool locks")
      .description("Get liquidity locks for tjhe pool with the given ID")

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

  def getAvgPoolSlippage: Endpoint[(PoolId, Long), HttpError, BigDecimal, Any] =
    baseEndpoint.get
      .in(PathPrefix / "pool" / path[PoolId].description("Asset reference") / "slippage")
      .in(query[Long]("blockDepth").default(10L).validate(Validator.min(1L)).validate(Validator.max(128L)))
      .out(jsonBody[BigDecimal])
      .tag(Group)
      .name("Pool slippage")
      .description("Get average slippage by pool")
}
