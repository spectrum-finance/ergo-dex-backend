package org.ergoplatform.common

import io.circe.generic.auto._
import _root_.sttp.model.StatusCode
import _root_.sttp.tapir._
import _root_.sttp.tapir.generic.auto._
import _root_.sttp.tapir.json.circe._

package object http {

  val baseEndpoint: Endpoint[Unit, HttpError, Unit, Any] = endpoint.errorOut(
    oneOf[HttpError](
      oneOfMapping(StatusCode.NotFound, jsonBody[HttpError.NotFound].description("not found")),
      oneOfMapping(StatusCode.Unauthorized, jsonBody[HttpError.Unauthorized].description("unauthorized")),
      oneOfMapping(StatusCode.NoContent, emptyOutputAs(HttpError.NoContent)),
      oneOfDefaultMapping(jsonBody[HttpError.Unknown].description("unknown"))
    )
  )
}
