package org.ergoplatform.dex.markets.api.v1.endpoints

import org.ergoplatform.common.http.HttpError
import org.ergoplatform.common.models.TimeWindow
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.dex.markets.api.v1.models.amm.{PlatformSummary, PoolSummary}
import sttp.tapir.{Endpoint, path}
import sttp.tapir._
import sttp.tapir.json.circe.jsonBody

final class AmmAnalyticsEndpoints {

  val pathPrefix = "amm"
  val group      = "ammStats"

  def endpoints: List[Endpoint[_, _, _, _]] = getPlatformStats :: getPoolStats :: Nil

  def getPoolStats: Endpoint[(PoolId, TimeWindow), HttpError, PoolSummary, Any] =
    baseEndpoint.get
      .in(pathPrefix / "pool" / path[PoolId].description("Asset reference") / "stats")
      .in(timeWindow)
      .out(jsonBody[PoolSummary])
      .tag(group)
      .name("Pool stats")
      .description("Get statistics on a pool with the given ID")

  def getPlatformStats: Endpoint[TimeWindow, HttpError, PlatformSummary, Any] =
    baseEndpoint.get
      .in(pathPrefix / "platform" / "stats")
      .in(timeWindow)
      .out(jsonBody[PlatformSummary])
      .tag(group)
      .name("Platform stats")
      .description("Get statistics on whole AMM")
}
