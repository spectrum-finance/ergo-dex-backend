package org.ergoplatform.dex.protocol.amm

import org.ergoplatform.ergo.ErgoTreeTemplate
import org.ergoplatform.dex.protocol.amm.AMMType.T2T_CFMM

trait ContractTemplates[CF <: AMMType] {

  def deposit: ErgoTreeTemplate

  def redeem: ErgoTreeTemplate

  def swap: ErgoTreeTemplate
}

object ContractTemplates {

  implicit val t2tCfmmTemplates: ContractTemplates[T2T_CFMM] = T2tCfmmTemplates
}
