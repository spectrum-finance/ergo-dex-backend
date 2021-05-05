package org.ergoplatform.dex.protocol.amm

import org.ergoplatform.dex.SErgoTree
import org.ergoplatform.dex.protocol.amm.AmmContractType.T2tCfmm

trait AmmProxyContracts[CT <: AmmContractType] {

  def isDeposit(ergoTree: SErgoTree): Boolean

  def isRedeem(ergoTree: SErgoTree): Boolean

  def isSwap(ergoTree: SErgoTree): Boolean
}

object AmmProxyContracts {

  implicit object T2tCfmmProxyContracts extends AmmProxyContracts[T2tCfmm] {

    def isDeposit(ergoTree: SErgoTree): Boolean = ???

    def isRedeem(ergoTree: SErgoTree): Boolean = ???

    def isSwap(ergoTree: SErgoTree): Boolean = ???
  }
}
