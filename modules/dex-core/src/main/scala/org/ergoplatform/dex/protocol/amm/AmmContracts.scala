package org.ergoplatform.dex.protocol.amm

import sigmastate.Values.ErgoTree

trait AmmContracts[CT <: AMMType] {

  def pool: ErgoTree
}
