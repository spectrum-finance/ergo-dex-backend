package org.ergoplatform.dex.domain.amm.state

import derevo.cats.show
import derevo.circe.{decoder, encoder}
import derevo.derive
import scodec.Codec
import sttp.tapir.{Schema, Validator}
import tofu.logging.derivation.loggable

/** Predicted entity state.
  */
@derive(loggable, encoder, decoder, show)
final case class Predicted[T](predicted: T)

object Predicted {
  implicit def schema[T: Schema]: Schema[Predicted[T]]          = Schema.derived[Predicted[T]]
  implicit def validator[T: Validator]: Validator[Predicted[T]] = Validator.pass

  implicit def codec[T: Codec]: Codec[Predicted[T]] = implicitly[Codec[T]].as[Predicted[T]]
}
