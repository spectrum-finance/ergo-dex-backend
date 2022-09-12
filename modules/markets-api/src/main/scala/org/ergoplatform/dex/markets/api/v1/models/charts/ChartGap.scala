package org.ergoplatform.dex.markets.api.v1.models.charts

import cats.Show
import doobie.util.Put
import enumeratum.values.{StringEnum, StringEnumEntry}
import io.circe.{Decoder, Encoder}
import sttp.tapir.{Codec, Schema}
import cats.syntax.either._
import org.ergoplatform.dex.markets.api.v1.services.{Utc, Zone}
import sttp.tapir.Schema.SName
import sttp.tapir.generic.Derived
import tofu.logging.Loggable
import sttp.tapir.generic.auto._

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.time.temporal.ChronoUnit
import scala.concurrent.duration.{DurationInt, FiniteDuration}

sealed abstract class ChartGap(
  val value: String,
  val dateFormat: String,
  val timeWindow: FiniteDuration,
  val pgValue: Int, /** This value is used only for minutes aggregation. E.g. 5 means the 5 minutes gap , 15 - 15 means gap. */
  val minimalGap: Long,
  val javaDateFormat: String
) extends StringEnumEntry

object ChartGap extends StringEnum[ChartGap] {
  case object Gap5min extends ChartGap("5min", "YYYY-mm-dd HH24:MI", 5.minutes, 5, 1.hour.toMillis, "yyyy-MM-dd HH:mm")

  case object Gap15min
    extends ChartGap("15min", "YYYY-mm-dd HH24:MI", 15.minutes, 15, 6.hours.toMillis, "yyyy-MM-dd HH:mm")

  case object Gap30min
    extends ChartGap("30min", "YYYY-mm-dd HH24:MI", 30.minutes, 30, 1.day.toMillis, "yyyy-MM-dd HH:mm")
  case object Gap1h extends ChartGap("1h", "YYYY-mm-dd HH24", 1.hour, 1, 1.day.toMillis, "yyyy-MM-dd HH")
  case object Gap1d extends ChartGap("1d", "YYYY-mm-dd", 1.day, 1, 7.days.toMillis, "yyyy-MM-dd")
  case object Gap1m extends ChartGap("1m", "YYYY-mm", 30.days, 1, 180.days.toMillis, "yyyy-MM")
  case object Gap1y extends ChartGap("1y", "YYYY", 365.days, 1, 1095.days.toMillis, "yyyy")

  val values = findValues

  implicit val put: Put[ChartGap] = implicitly[Put[String]].contramap(_.dateFormat)

  implicit val schema: Schema[ChartGap] = implicitly[Derived[Schema[ChartGap]]].value
    .modify(_.value)(
      _.description("The gap's value. min means minute, h means hour, d means day, m means month, y means year.")
    )
    .default(ChartGap.Gap1h)
    .name(SName("Chart gap"))

  implicit val decoder: Decoder[ChartGap] =
    Decoder.decodeString.emap(s => Either.catchNonFatal(ChartGap.withValue(s)).leftMap(_.getMessage))

  implicit val encoder: Encoder[ChartGap] = Encoder.encodeString.contramap(_.value)

  implicit def plainCodec: Codec.PlainCodec[ChartGap] = Codec.stringCodec(s => ChartGap.withValue(s))

  implicit val show: Show[ChartGap] = _.value

  implicit def loggable: Loggable[ChartGap] = Loggable.show

  def round(gap: ChartGap, from: Long): Long =
    gap match {
      case ChartGap.Gap5min | ChartGap.Gap15min | ChartGap.Gap30min | ChartGap.Gap1h =>
        from / gap.timeWindow.toMillis * gap.timeWindow.toMillis
      case ChartGap.Gap1d =>
        LocalDateTime
          .ofInstant(Instant.ofEpochMilli(from), Zone)
          .withHour(0)
          .withMinute(0)
          .withSecond(0)
          .truncatedTo(ChronoUnit.DAYS)
          .toEpochSecond(ZoneOffset.UTC) * 1000
      case ChartGap.Gap1m =>
        LocalDateTime
          .ofInstant(Instant.ofEpochMilli(from), Zone)
          .withDayOfMonth(1)
          .withHour(0)
          .withMinute(0)
          .withSecond(0)
          .toEpochSecond(ZoneOffset.UTC) * 1000
      case ChartGap.Gap1y =>
        LocalDateTime
          .ofInstant(Instant.ofEpochMilli(from), Zone)
          .withDayOfYear(1)
          .withDayOfMonth(1)
          .withHour(0)
          .withMinute(0)
          .withSecond(0)
          .toEpochSecond(ZoneOffset.UTC) * 1000
    }

  def updateWithGap(gap: ChartGap, time: Long): Long =
    gap match {
      case ChartGap.Gap5min | ChartGap.Gap15min | ChartGap.Gap30min | ChartGap.Gap1h =>
        time + gap.timeWindow.toMillis
      case ChartGap.Gap1d =>
        LocalDateTime
          .ofInstant(Instant.ofEpochMilli(time), Zone)
          .plusDays(1)
          .toEpochSecond(Utc) * 1000
      case ChartGap.Gap1m =>
        LocalDateTime
          .ofInstant(Instant.ofEpochMilli(time), Zone)
          .plusMonths(1)
          .toEpochSecond(Utc) * 1000
      case ChartGap.Gap1y =>
        LocalDateTime
          .ofInstant(Instant.ofEpochMilli(time), Zone)
          .plusYears(1)
          .toEpochSecond(Utc) * 1000
    }
}
