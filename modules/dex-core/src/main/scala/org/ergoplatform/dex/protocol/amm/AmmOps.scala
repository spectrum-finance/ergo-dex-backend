package org.ergoplatform.dex.protocol.amm

import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.domain.network.Output
import org.ergoplatform.dex.protocol.amm.AmmContractType.T2tCfmm

trait AmmOps[CT <: AmmContractType] {

  def parseDeposit(box: Output): Option[Deposit]

  def parseRedeem(box: Output): Option[Redeem]

  def parseSwap(box: Output): Option[Swap]
}

object AmmOps {

  implicit def t2tCfmmOps(implicit e: ErgoAddressEncoder): AmmOps[T2tCfmm] = new T2tCfmmOps()
}
