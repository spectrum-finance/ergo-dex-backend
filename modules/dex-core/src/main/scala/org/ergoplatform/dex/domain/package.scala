package org.ergoplatform.dex

import derevo.derive
import org.ergoplatform.ergo.TokenId
import tofu.logging.derivation.loggable

package object domain {
  @derive(loggable)
  final case class PairId(quoteId: TokenId, baseId: TokenId)
}
