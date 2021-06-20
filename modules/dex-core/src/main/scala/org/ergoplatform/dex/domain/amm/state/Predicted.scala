package org.ergoplatform.dex.domain.amm.state

import derevo.circe.{decoder, encoder}
import derevo.derive
import sttp.tapir.{Schema, Validator}
import tofu.logging.derivation.loggable

/** Predicted entity state.
  */
@derive(loggable, encoder, decoder)
final case class Predicted[T](predicted: T)

object Predicted {
  implicit def schema[T: Schema]: Schema[Predicted[T]]          = Schema.derived[Predicted[T]]
  implicit def validator[T: Validator]: Validator[Predicted[T]] = Validator.pass
}
