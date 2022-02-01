package org.ergoplatform.ergo.state

import derevo.cats.show
import derevo.circe.{decoder, encoder}
import derevo.derive
import scodec.Codec
import scodec.codecs.int64
import sttp.tapir.{Schema, Validator}
import tofu.logging.derivation.loggable

/** Off-chain entity state.
  */
@derive(loggable, encoder, decoder, show)
final case class PredictedIndexed[T](entity: T, lastGix: Long)

object PredictedIndexed {
  implicit def schema[T: Schema]: Schema[PredictedIndexed[T]]          = Schema.derived[PredictedIndexed[T]]
  implicit def validator[T: Validator]: Validator[PredictedIndexed[T]] = Validator.pass

  implicit def codec[T: Codec]: Codec[PredictedIndexed[T]] = (implicitly[Codec[T]] :: int64).as[PredictedIndexed[T]]
}

