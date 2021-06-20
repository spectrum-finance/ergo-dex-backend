package org.ergoplatform.dex.resolver.http

import org.ergoplatform.dex.domain.amm.state.Predicted
import org.ergoplatform.dex.domain.amm.{CFMMPool, PoolId}
import sttp.tapir._
import sttp.tapir.json.circe._

final class Endpoints(basePathPrefix: String) {

  private val baseEndpoint = endpoint.in(basePathPrefix / "cfmm")

  val endpoints: List[Endpoint[_, Unit, _, Any]] = getPool :: putPredicted :: Nil

  def getPool: Endpoint[PoolId, Unit, CFMMPool, Any] =
    baseEndpoint.get
      .in(path[PoolId])
      .out(jsonBody[CFMMPool])

  def putPredicted: Endpoint[Predicted[CFMMPool], Unit, Unit, Any] =
    baseEndpoint.post
      .in(jsonBody[Predicted[CFMMPool]])
}
