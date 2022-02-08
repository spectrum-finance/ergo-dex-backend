package org.ergoplatform.ergo.domain

import derevo.cats.show
import derevo.circe.{decoder, encoder}
import derevo.derive
import scodec.Codec
import scodec.codecs.{int32, int64}
import sttp.tapir.{Schema, Validator}
import tofu.logging.derivation.loggable

@derive(loggable, encoder, decoder, show)
final case class LedgerMetadata(gix: Long, height: Int)

object LedgerMetadata {

  implicit def schema: Schema[LedgerMetadata]       = Schema.derived
  implicit def validator: Validator[LedgerMetadata] = Validator.pass

  implicit def codec: Codec[LedgerMetadata] =
    (int64 :: int32).as[LedgerMetadata]

  def apply(out: SettledOutput): LedgerMetadata = LedgerMetadata(out.gix, out.settlementHeight)
}
