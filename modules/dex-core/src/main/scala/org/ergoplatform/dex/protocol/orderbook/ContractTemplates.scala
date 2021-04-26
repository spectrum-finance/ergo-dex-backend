package org.ergoplatform.dex.protocol.orderbook

import org.ergoplatform.contracts.DexLimitOrderContracts._
import org.ergoplatform.dex.ErgoTreeTemplate

trait ContractTemplates[CT <: OrderContractType] {
  val ask: ErgoTreeTemplate
  val bid: ErgoTreeTemplate
}

object ContractTemplates {

  implicit object limitOrder extends ContractTemplates[OrderContractType.LimitOrder] {
    val ask: ErgoTreeTemplate = ErgoTreeTemplate.fromBytes(sellerContractErgoTreeTemplate)
    val bid: ErgoTreeTemplate = ErgoTreeTemplate.fromBytes(buyerContractErgoTreeTemplate)
  }
}
