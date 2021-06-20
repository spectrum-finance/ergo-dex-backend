package org.ergoplatform.dex.domain.amm.state

import derevo.circe.{decoder, encoder}
import derevo.derive
import tofu.logging.derivation.loggable

/** On-chain entity state.
  */
@derive(loggable, encoder, decoder)
final case class OnChain[T](onChain: T)
