package org.ergoplatform.graphite

import cats.Show

sealed trait GraphitePoint {
  def format: String

  def transformation(prefix: String): GraphitePoint
}

object GraphitePoint {

  final case class GraphitePointTs(
    path: String,
    value: Double
  ) extends GraphitePoint {
    def format: String = s"$path:${value.round}|ms\n"

    def transformation(prefix: String): GraphitePoint =
      this.copy(path = s"$prefix.$path")
  }

  final case class GraphitePointCount(
    path: String,
    value: Double
  ) extends GraphitePoint {
    def format: String = s"$path.count:${value.round}|c\n"

    def transformation(prefix: String): GraphitePoint =
      this.copy(path = s"$prefix.$path")
  }

  implicit val show: Show[GraphitePoint] = _.format

}
