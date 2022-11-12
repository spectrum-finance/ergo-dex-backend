package org.ergoplatform.graphite

import java.time.{LocalDateTime, ZoneOffset}

import cats.Show

final case class GraphitePoint(
  path: String,
  value: Double,
  ts: Long
)

object GraphitePoint {

  implicit val show: Show[GraphitePoint] = format(_)

  def apply(path: String, value: Double, dt: LocalDateTime): GraphitePoint =
    GraphitePoint(path, value, dt.toInstant(ZoneOffset.UTC).getEpochSecond)

  def format(point: Seq[GraphitePoint]): String =
    point.map(format).mkString

  def format(point: GraphitePoint): String =
    s"${point.path} ${point.value.round} ${point.ts}\n"
}
