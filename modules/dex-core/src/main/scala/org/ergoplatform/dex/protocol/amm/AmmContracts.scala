package org.ergoplatform.dex.protocol.amm

import sigmastate.Values.ErgoTree

trait AmmContracts[CT <: AmmContractType] {

  def pool: ErgoTree
}
