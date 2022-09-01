package org.ergoplatform.dex.markets.api.v1.models.amm

import cats.syntax.either._
import enumeratum.{Enum, EnumEntry}
import io.circe.{Decoder, Encoder}

import java.util.{GregorianCalendar, TimeZone}

sealed abstract class ApiMonth(
  override val entryName: String,
  val from21: Long,
  val to21: Long,
  val from22: Long,
  val to22: Long
) extends EnumEntry

object ApiMonth extends Enum[ApiMonth] {
  val values = findValues

  def setDate(year: Int, month: Int): Long = {
    val calendar = new GregorianCalendar()
    val tz       = TimeZone.getTimeZone("GMT")
    calendar.setTimeZone(tz)
    calendar.set(year, month, 1, 0, 0, 0)
    calendar.getTime.getTime
  }

  def setDatePrevMs(year: Int, month: Int): Long =
    setDate(year, month) - 1

  case object January
    extends ApiMonth("january", setDate(2021, 0), setDatePrevMs(2021, 1), setDate(2022, 0), setDatePrevMs(2022, 1))

  case object February
    extends ApiMonth("february", setDate(2021, 1), setDatePrevMs(2021, 2), setDate(2022, 1), setDatePrevMs(2022, 2))

  case object March
    extends ApiMonth("march", setDate(2021, 2), setDatePrevMs(2021, 3), setDate(2022, 2), setDatePrevMs(2022, 3))

  case object April
    extends ApiMonth("april", setDate(2021, 3), setDatePrevMs(2021, 4), setDate(2022, 3), setDatePrevMs(2022, 4))

  case object May
    extends ApiMonth("may", setDate(2021, 4), setDatePrevMs(2021, 5), setDate(2022, 4), setDatePrevMs(2022, 5))

  case object June
    extends ApiMonth("june", setDate(2021, 5), setDatePrevMs(2021, 6), setDate(2022, 5), setDatePrevMs(2022, 6))

  case object July
    extends ApiMonth("july", setDate(2021, 6), setDatePrevMs(2021, 7), setDate(2022, 6), setDatePrevMs(2022, 7))

  case object August
    extends ApiMonth("august", setDate(2021, 7), setDatePrevMs(2021, 8), setDate(2022, 7), setDatePrevMs(2022, 8))

  case object September
    extends ApiMonth("september", setDate(2021, 8), setDatePrevMs(2021, 9), setDate(2022, 8), setDatePrevMs(2022, 9))

  case object October
    extends ApiMonth("october", setDate(2021, 9), setDatePrevMs(2021, 10), setDate(2022, 9), setDatePrevMs(2022, 10))

  case object November
    extends ApiMonth("november", setDate(2021, 10), setDatePrevMs(2021, 11), setDate(2022, 10), setDatePrevMs(2022, 11))

  case object December
    extends ApiMonth("december", setDate(2021, 11), setDatePrevMs(2021, 12), setDate(2022, 11), setDatePrevMs(2022, 12))

  implicit val encoder: Encoder[ApiMonth] = Encoder[String].contramap(_.entryName)

  implicit val decoder: Decoder[ApiMonth] =
    Decoder[String].emap(s => withNameInsensitiveEither(s).leftMap(_.getMessage()))

}
