package org.ergoplatform.common

import io.circe.generic.auto._
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._

package object http {

  val baseEndpoint: Endpoint[Unit, HttpError, Unit, Any] = endpoint.errorOut(
    oneOf[HttpError](
      oneOfMapping(StatusCode.NotFound, jsonBody[NotFound].description("not found")),
      oneOfMapping(StatusCode.Unauthorized, jsonBody[Unauthorized].description("unauthorized")),
      oneOfMapping(StatusCode.NoContent, emptyOutputAs(NoContent)),
      oneOfDefaultMapping(jsonBody[Unknown].description("unknown"))
    )
  )
}
