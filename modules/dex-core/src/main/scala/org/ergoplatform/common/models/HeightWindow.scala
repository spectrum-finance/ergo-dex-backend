package org.ergoplatform.common.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import sttp.tapir.Schema
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
final case class HeightWindow(from: Option[Long], to: Option[Long])

object HeightWindow {

  implicit val schema: Schema[HeightWindow] = Schema.derived
}
