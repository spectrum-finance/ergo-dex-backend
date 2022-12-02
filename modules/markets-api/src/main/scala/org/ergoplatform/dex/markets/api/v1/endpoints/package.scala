package org.ergoplatform.dex.markets.api.v1

import cats.implicits.catsSyntaxOptionId
import org.ergoplatform.common.http.HttpError
import org.ergoplatform.common.models.{HeightWindow, Paging, TimeWindow}
import sttp.model.StatusCode
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.{Endpoint, EndpointInput, Schema, ValidationError, Validator, emptyOutputAs, endpoint, oneOf, oneOfDefaultMapping, oneOfMapping, query}
import sttp.tapir.generic.auto._
import scala.concurrent.duration.FiniteDuration

package object endpoints {

  implicit def schemaBigInt: Schema[BigInt] = Schema.schemaForBigDecimal.map(_.toBigIntExact)(BigDecimal(_))

  val VersionPrefix = "v1"

  val baseEndpoint: Endpoint[Unit, HttpError, Unit, Any] =
    endpoint
      .in(VersionPrefix)
      .errorOut(
        oneOf[HttpError](
          oneOfMapping(StatusCode.NotFound, jsonBody[HttpError.NotFound].description("not found")),
          oneOfMapping(StatusCode.NoContent, emptyOutputAs(HttpError.NoContent)),
          oneOfDefaultMapping(jsonBody[HttpError.Unknown].description("unknown"))
        )
      )

  def paging: EndpointInput[Paging] =
    (query[Option[Int]]("offset").validateOption(Validator.min(0)) and
      query[Option[Int]]("limit")
        .validateOption(Validator.min(1))
        .validateOption(Validator.max(Int.MaxValue)))
      .map { input =>
        Paging(input._1.getOrElse(0), input._2.getOrElse(20))
      } { case Paging(offset, limit) => offset.some -> limit.some }

  def timeWindow: EndpointInput[TimeWindow] =
    (query[Option[Long]]("from")
      .description("Window lower bound (UNIX timestamp millis)")
      .validateOption(Validator.min(0L)) and
      query[Option[Long]]("to")
        .description("Window upper bound (UNIX timestamp millis)")
        .validateOption(Validator.min(0L)))
      .map { input =>
        TimeWindow(input._1, input._2)
      } { case TimeWindow(from, to) => from -> to }

  def timeWindow(maxInterval: FiniteDuration): EndpointInput[TimeWindow] =
    (query[Long]("from")
      .description("Window lower bound (UNIX timestamp millis)")
      .validate(Validator.min(0L)) and
      query[Long]("to")
        .description("Window upper bound (UNIX timestamp millis)")
        .validate(Validator.min(0L)))
      .validate(Validator.custom[(Long, Long)] { case (from, to) =>
        if (to - from < maxInterval.toMillis) List.empty
        else List(ValidationError.Custom((from, to), "time window exceeded"))
      })
      .map { input =>
        TimeWindow(Some(input._1), Some(input._2))
      } { case TimeWindow(from, to) => from.getOrElse(0L) -> to.getOrElse(0L) }

  def heightWindow: EndpointInput[HeightWindow] =
    (query[Option[Long]]("from")
      .description("Window lower bound (Block height)")
      .validateOption(Validator.min(0L)) and
      query[Option[Long]]("to")
        .description("Window upper bound (Block height)")
        .validateOption(Validator.min(0L)))
      .map { input =>
        HeightWindow(input._1, input._2)
      } { case HeightWindow(from, to) => from -> to }

}
