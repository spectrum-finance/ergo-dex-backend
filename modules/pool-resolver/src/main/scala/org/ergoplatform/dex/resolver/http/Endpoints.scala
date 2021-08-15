package org.ergoplatform.dex.resolver.http

import org.ergoplatform.common.http.{HttpError, baseEndpoint}
import org.ergoplatform.dex.domain.amm.state.Predicted
import org.ergoplatform.dex.domain.amm.{CFMMPool, PoolId}
import org.ergoplatform.ergo.BoxId
import sttp.tapir._
import sttp.tapir.json.circe._

object Endpoints {

  private val endpoint = baseEndpoint.in("cfmm")

  val endpoints: List[Endpoint[_, _, _, _]] = resolve :: putPredicted :: Nil

  def resolve: Endpoint[PoolId, HttpError, CFMMPool, Any] =
    endpoint.get
      .in("resolve" / path[PoolId])
      .out(jsonBody[CFMMPool])

  def putPredicted: Endpoint[Predicted[CFMMPool], HttpError, Unit, Any] =
    endpoint.post
      .in("predicted")
      .in(jsonBody[Predicted[CFMMPool]])

  def invalidate: Endpoint[BoxId, HttpError, Unit, Any] =
    endpoint.post
      .in("invalidate" / path[BoxId])
}
