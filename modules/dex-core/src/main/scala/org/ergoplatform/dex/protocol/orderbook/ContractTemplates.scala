package org.ergoplatform.dex.protocol.orderbook

import org.ergoplatform.contracts.DexLimitOrderContracts._
import org.ergoplatform.ergo.ErgoTreeTemplate

trait ContractTemplates[CT <: OrderContractFamily] {
  val ask: ErgoTreeTemplate
  val bid: ErgoTreeTemplate
}

object ContractTemplates {

  implicit object limitOrder extends ContractTemplates[OrderContractFamily.LimitOrders] {
    val ask: ErgoTreeTemplate = ErgoTreeTemplate.fromBytes(sellerContractErgoTreeTemplate)
    val bid: ErgoTreeTemplate = ErgoTreeTemplate.fromBytes(buyerContractErgoTreeTemplate)
  }
}
