package org.ergoplatform.dex.markets.api.v1.endpoints

import org.ergoplatform.common.http.HttpError
import sttp.tapir._

object DocsEndpoints {

  private val PathPrefix = "docs"
  private val Group = "docs"

  def endpoints: List[Endpoint[_, _, _, _]] = apiSpecDef :: Nil

  def apiSpecDef: Endpoint[Unit, HttpError, String, Any] =
    baseEndpoint
      .in(PathPrefix / "openapi")
      .out(plainBody[String])
      .tag(Group)
      .name("Openapi route")
      .description("Allow to get openapi.yaml")
}
