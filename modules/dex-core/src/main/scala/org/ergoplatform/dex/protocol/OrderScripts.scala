package org.ergoplatform.dex.protocol

import org.ergoplatform.dex.ErgoTree

trait OrderScripts {

  def isAsk(ergoTree: ErgoTree): Boolean

  def isBid(ergoTree: ErgoTree): Boolean
}
