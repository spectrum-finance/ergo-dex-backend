package org.ergoplatform.dex.domain.amm.state

import derevo.derive
import tofu.logging.derivation.loggable

/** Predicted entity state.
  */
@derive(loggable)
final case class Predicted[T](entity: T)
