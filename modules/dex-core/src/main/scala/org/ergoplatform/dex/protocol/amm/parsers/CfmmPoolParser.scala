package org.ergoplatform.dex.protocol.amm.parsers

import org.ergoplatform.dex.domain.OnChain
import org.ergoplatform.dex.domain.amm.CfmmPool
import org.ergoplatform.dex.protocol.amm.AmmContractType.{CfmmFamily, T2tCfmm}
import org.ergoplatform.ergo.models.Output

trait CfmmPoolParser[CF <: CfmmFamily] {

  def parse(box: Output): Option[OnChain[CfmmPool]]
}

object CfmmPoolParser {
  implicit val t2tCfmmPoolParser: CfmmPoolParser[T2tCfmm] = T2tCfmmPoolParser
}
