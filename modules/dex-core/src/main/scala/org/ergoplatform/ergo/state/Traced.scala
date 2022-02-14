package org.ergoplatform.ergo.state

import derevo.cats.show
import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.ergo.BoxId
import scodec.Codec
import sttp.tapir.{Schema, Validator}
import tofu.logging.derivation.loggable

/** State of an entity which tracks boxId of it's previous version.
  */
@derive(loggable, encoder, decoder, show)
final case class Traced[T](state: T, predecessorBoxId: BoxId)

object Traced {
  implicit def schema[T: Schema]: Schema[Traced[T]]          = Schema.derived[Traced[T]]
  implicit def validator[T: Validator]: Validator[Traced[T]] = Validator.pass

  implicit def codec[T: Codec]: Codec[Traced[T]] = (implicitly[Codec[T]] :: implicitly[Codec[BoxId]]).as[Traced[T]]
}
