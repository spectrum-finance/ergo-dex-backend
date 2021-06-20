package org.ergoplatform.dex.domain.amm.state

import derevo.circe.{decoder, encoder}
import derevo.derive
import tofu.logging.derivation.loggable

/** Confirmed on-chain entity state.
  */
@derive(loggable, encoder, decoder)
final case class Confirmed[T](confirmed: T)
