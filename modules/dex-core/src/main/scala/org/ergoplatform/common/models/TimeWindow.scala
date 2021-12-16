package org.ergoplatform.common.models

import cats.Monoid
import cats.syntax.semigroup._

final case class TimeWindow(from: Option[Long], to: Option[Long])

object TimeWindow {

  val empty: TimeWindow = TimeWindow(None, None)

  implicit val monoid: Monoid[TimeWindow] =
    Monoid.instance(empty, (w0, w1) => w0.copy(w0.to |+| w1.to, w0.from |+| w1.from))
}
