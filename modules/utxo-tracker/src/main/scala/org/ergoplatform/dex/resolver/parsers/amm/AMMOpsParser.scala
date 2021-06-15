package org.ergoplatform.dex.resolver.parsers.amm

import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.protocol.amm.AMMType.{CFMMFamily, T2TCFMM}
import org.ergoplatform.dex.protocol.amm.ContractTemplates
import org.ergoplatform.ergo.models.Output

trait AMMOpsParser[CT <: CFMMFamily] {

  def deposit(box: Output): Option[Deposit]

  def redeem(box: Output): Option[Redeem]

  def swap(box: Output): Option[Swap]
}

object AMMOpsParser {

  implicit def t2tCfmmOps(implicit ts: ContractTemplates[T2TCFMM], e: ErgoAddressEncoder): AMMOpsParser[T2TCFMM] =
    new T2TCfmmOpsParser()
}
