package org.ergoplatform.dex.protocol.amm

import org.ergoplatform.dex.ErgoTreeTemplate
import org.ergoplatform.dex.protocol.amm.AmmContractType.T2tCfmm

trait ContractTemplates[CF <: AmmContractType] {

  def deposit: ErgoTreeTemplate

  def redeem: ErgoTreeTemplate

  def swap: ErgoTreeTemplate
}

object ContractTemplates {

  implicit val t2tCfmmTemplates: ContractTemplates[T2tCfmm] = T2tCfmmTemplates
}
