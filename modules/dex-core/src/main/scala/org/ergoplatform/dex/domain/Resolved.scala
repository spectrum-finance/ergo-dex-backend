package org.ergoplatform.dex.domain

import derevo.derive
import tofu.logging.derivation.loggable

/** Resolved entity state.
 */
@derive(loggable)
final case class Resolved[T](entity: T)
