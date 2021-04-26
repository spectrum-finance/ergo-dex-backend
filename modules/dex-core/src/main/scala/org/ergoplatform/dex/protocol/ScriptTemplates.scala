package org.ergoplatform.dex.protocol

import org.ergoplatform.contracts.DexLimitOrderContracts._
import org.ergoplatform.dex.ErgoTreeTemplate

trait ScriptTemplates[CT <: ContractType] {
  val ask: ErgoTreeTemplate
  val bid: ErgoTreeTemplate
}

object ScriptTemplates {

  implicit object limitOrder extends ScriptTemplates[ContractType.LimitOrder] {
    val ask: ErgoTreeTemplate = ErgoTreeTemplate.fromBytes(sellerContractErgoTreeTemplate)
    val bid: ErgoTreeTemplate = ErgoTreeTemplate.fromBytes(buyerContractErgoTreeTemplate)
  }
}
