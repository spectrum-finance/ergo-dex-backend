package org.ergoplatform.dex.domain.amm.state

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
final case class OffChainIndexed[T](entity: T, lastGix: Long)

object OffChainIndexed {
  implicit def schema[T: Schema]: Schema[OffChainIndexed[T]]          = Schema.derived[OffChainIndexed[T]]
  implicit def validator[T: Validator]: Validator[OffChainIndexed[T]] = Validator.pass

  implicit def codec[T: Codec]: Codec[OffChainIndexed[T]] = (implicitly[Codec[T]] :: int64).as[OffChainIndexed[T]]
}

