package org.ergoplatform.dex.protocol.amm

import org.ergoplatform.dex.SErgoTree
import org.ergoplatform.dex.protocol.orderbook.OrderContractType

trait AmmContracts[CT <: OrderContractType] {

  def isDeposit(ergoTree: SErgoTree): Boolean

  def isRedeem(ergoTree: SErgoTree): Boolean

  def isSwap(ergoTree: SErgoTree): Boolean
}
