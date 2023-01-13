package org.ergoplatform.graphite

import cats.Show

sealed trait GraphitePoint {
  def format: String

  def transformation(prefix: String): GraphitePoint
}

object GraphitePoint {

  final case class GraphitePointUdp(
    path: String,
    value: Double,
    ts: Long
  ) extends GraphitePoint {
    def format: String = s"$path ${value.round} $ts\n"

    def transformation(prefix: String): GraphitePoint =
      this.copy(path = s"$prefix.$path")
  }

  implicit val show: Show[GraphitePoint] = _.format

}
