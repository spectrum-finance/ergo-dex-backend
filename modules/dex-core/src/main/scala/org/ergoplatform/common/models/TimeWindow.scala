package org.ergoplatform.common.models

import cats.Monoid
import cats.syntax.semigroup._
import derevo.circe.{decoder, encoder}
import derevo.derive
import sttp.tapir.Schema

@derive(encoder, decoder)
final case class TimeWindow(from: Option[Long], to: Option[Long])

object TimeWindow {

  val empty: TimeWindow = TimeWindow(None, None)

  implicit val monoid: Monoid[TimeWindow] =
    Monoid.instance(empty, (w0, w1) => w0.copy(w0.to |+| w1.to, w0.from |+| w1.from))

  implicit val schema: Schema[TimeWindow] = Schema.derived
}
