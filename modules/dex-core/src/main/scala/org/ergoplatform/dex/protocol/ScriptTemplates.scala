package org.ergoplatform.dex.protocol

import org.ergoplatform.contracts.DexLimitOrderContracts._
import org.ergoplatform.dex.HexString

trait ScriptTemplates[CT <: ContractType] {
  val ask: HexString
  val bid: HexString
}

object ScriptTemplates {

  implicit object limitOrder extends ScriptTemplates[ContractType.LimitOrder] {
    val ask: HexString = HexString.fromBytes(sellerContractErgoTreeTemplate)
    val bid: HexString = HexString.fromBytes(buyerContractErgoTreeTemplate)
  }
}
