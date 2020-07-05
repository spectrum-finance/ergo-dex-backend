package org.ergoplatform.dex.domain.syntax

import org.ergoplatform.ErgoBox
import org.ergoplatform.dex.BoxId

object boxId {

  implicit final class BoxIdOps(private val id: BoxId) extends AnyVal {
    def toErgo: ErgoBox.BoxId = ???
  }
}
