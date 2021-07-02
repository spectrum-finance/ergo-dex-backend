package org.ergoplatform.common

import io.circe.generic.auto._
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._

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
