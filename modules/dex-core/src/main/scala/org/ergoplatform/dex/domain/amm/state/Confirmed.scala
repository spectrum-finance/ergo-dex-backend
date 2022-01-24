package org.ergoplatform.dex.domain.amm.state

import derevo.cats.show
import derevo.circe.{decoder, encoder}
import derevo.derive
import scodec.Codec
import tofu.logging.derivation.loggable

/** Confirmed on-chain entity state.
  */
@derive(show, loggable, encoder, decoder)
final case class Confirmed[T](confirmed: T)

object Confirmed {
  implicit def codec[T: Codec]: Codec[Confirmed[T]] = implicitly[Codec[T]].as[Confirmed[T]]
}
