package org.ergoplatform.dex.tracker.parsers.amm

import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.domain.network.Output
import org.ergoplatform.dex.protocol.amm.AmmContractType.{CfmmFamily, T2tCfmm}
import org.ergoplatform.dex.protocol.amm.ContractTemplates

trait AmmOps[CT <: CfmmFamily] {

  def parseDeposit(box: Output): Option[Deposit]

  def parseRedeem(box: Output): Option[Redeem]

  def parseSwap(box: Output): Option[Swap]
}

object AmmOps {

  implicit def t2tCfmmOps(implicit ts: ContractTemplates[T2tCfmm], e: ErgoAddressEncoder): AmmOps[T2tCfmm] =
    new T2tCfmmOps()
}
