package org.ergoplatform.graphite

sealed trait PointTransformation {
  def apply(point: GraphitePoint): GraphitePoint
}

object PointTransformation {

  final case class PathPrefix(prefix: String) extends PointTransformation {

    def apply(point: GraphitePoint): GraphitePoint =
      point.copy(path = s"$prefix.${point.path}")
  }

  case object NoTransformation extends PointTransformation {

    def apply(point: GraphitePoint): GraphitePoint = point
  }
}
