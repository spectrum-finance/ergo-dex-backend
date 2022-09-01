package org.ergoplatform.dex.markets.api.v1

import org.ergoplatform.common.http.HttpError
import org.ergoplatform.common.models.{HeightWindow, TimeWindow}
import org.ergoplatform.dex.markets.api.v1.models.amm.{ApiMonth, ApiPool}
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

  def address: EndpointInput[String] =
    query[String]("address")
      .description("user address")

  def month: EndpointInput[ApiMonth] =
    query[String]("month")
      .description("month")
      .validate(Validator.enumeration(ApiMonth.values.map(_.entryName).toList))
      .map(r => ApiMonth.withName(r))(_.entryName)

  def year: EndpointInput[Int] =
    query[Int]("year")
      .description("Year")
      .validate(Validator.enumeration(List(2021, 2022)))

  def pool: EndpointInput[ApiPool] =
    query[String]("pool")
      .description("Pool")
      .validate(Validator.enumeration(ApiPool.values.map(_.entryName).toList))
      .map(r => ApiPool.withName(r))(_.entryName)

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
