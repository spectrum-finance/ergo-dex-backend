package org.ergoplatform.dex.protocol.amm

import org.ergoplatform.dex.SErgoTree
import org.ergoplatform.dex.protocol.amm.AmmContractType.T2tCfmm

trait AmmContracts[CT <: AmmContractType] {

  def isDeposit(ergoTree: SErgoTree): Boolean

  def isRedeem(ergoTree: SErgoTree): Boolean

  def isSwap(ergoTree: SErgoTree): Boolean
}

object AmmContracts {

  implicit object T2tCfmmContracts extends AmmContracts[T2tCfmm] {

    def isDeposit(ergoTree: SErgoTree): Boolean = ???

    def isRedeem(ergoTree: SErgoTree): Boolean = ???

    def isSwap(ergoTree: SErgoTree): Boolean = ???
  }
}
