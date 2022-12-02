package org.ergoplatform.common.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import sttp.tapir.Schema
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
final case class TimeInterval(from: Long, to: Long)

object TimeInterval {
  implicit val schema: Schema[TimeInterval] = Schema.derived
}
