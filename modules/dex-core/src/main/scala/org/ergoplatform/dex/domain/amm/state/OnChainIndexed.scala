package org.ergoplatform.dex.domain.amm.state

import derevo.cats.show
import derevo.circe.{decoder, encoder}
import derevo.derive
import scodec.Codec
import scodec.codecs.int64
import sttp.tapir.{Schema, Validator}
import tofu.logging.derivation.loggable

/** On-chain entity state.
  */
@derive(loggable, encoder, decoder, show)
final case class OnChainIndexed[T](entity: T, gix: Long)

object OnChainIndexed {
  implicit def schema[T: Schema]: Schema[OnChainIndexed[T]]          = Schema.derived[OnChainIndexed[T]]
  implicit def validator[T: Validator]: Validator[OnChainIndexed[T]] = Validator.pass

  implicit def codec[T: Codec]: Codec[OnChainIndexed[T]] = (implicitly[Codec[T]] :: int64).as[OnChainIndexed[T]]
}
