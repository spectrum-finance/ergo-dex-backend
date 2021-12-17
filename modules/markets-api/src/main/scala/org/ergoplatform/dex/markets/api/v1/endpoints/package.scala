package org.ergoplatform.dex.markets.api.v1

import org.ergoplatform.common.http.HttpError
import org.ergoplatform.common.models.TimeWindow
import sttp.model.StatusCode
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.{Endpoint, EndpointInput, Schema, Validator, emptyOutputAs, endpoint, oneOf, oneOfDefaultMapping, oneOfMapping, query}
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._

package object endpoints {

  implicit def schemaBigInt: Schema[BigInt] = Schema.schemaForBigDecimal.map(_.toBigIntExact)(BigDecimal(_))

  val baseEndpoint: Endpoint[Unit, HttpError, Unit, Any] =
    endpoint.errorOut(
      oneOf[HttpError](
        oneOfMapping(StatusCode.NotFound, jsonBody[HttpError.NotFound].description("not found")),
        oneOfMapping(StatusCode.NoContent, emptyOutputAs(HttpError.NoContent)),
        oneOfDefaultMapping(jsonBody[HttpError.Unknown].description("unknown"))
      )
    )

  def timeWindow: EndpointInput[TimeWindow] =
    (query[Option[Long]]("from").validateOption(Validator.min(0L)) and
      query[Option[Long]]("to").validateOption(Validator.min(0L)))
      .map { input =>
        TimeWindow(input._1, input._2)
      } { case TimeWindow(from, to) => from -> to }
}
