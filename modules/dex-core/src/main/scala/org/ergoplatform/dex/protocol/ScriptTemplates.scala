package org.ergoplatform.dex.protocol

import org.ergoplatform.contracts.DexLimitOrderContracts._
import org.ergoplatform.dex.HexString
import tofu.Context

final case class ScriptTemplates(
  limitOrderAsk: HexString,
  limitOrderBid: HexString
)

object ScriptTemplates extends Context.Companion[ScriptTemplates] {

  def default: ScriptTemplates = ScriptTemplates(
    HexString.fromBytes(sellerContractErgoTreeTemplate),
    HexString.fromBytes(buyerContractErgoTreeTemplate)
  )
}
