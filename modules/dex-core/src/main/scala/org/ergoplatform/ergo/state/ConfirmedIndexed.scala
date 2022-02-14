package org.ergoplatform.ergo.state

import derevo.cats.show
import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.ergo.domain.LedgerMetadata
import scodec.Codec
import scodec.codecs.int64
import sttp.tapir.{Schema, Validator}
import tofu.logging.derivation.loggable

/** On-chain entity state.
  */
@derive(loggable, encoder, decoder, show)
final case class ConfirmedIndexed[T](entity: T, meta: LedgerMetadata)

object ConfirmedIndexed {
  implicit def schema[T: Schema]: Schema[ConfirmedIndexed[T]]          = Schema.derived
  implicit def validator[T: Validator]: Validator[ConfirmedIndexed[T]] = Validator.pass

  implicit def codec[T: Codec]: Codec[ConfirmedIndexed[T]] =
    (implicitly[Codec[T]] :: implicitly[Codec[LedgerMetadata]]).as[ConfirmedIndexed[T]]
}
