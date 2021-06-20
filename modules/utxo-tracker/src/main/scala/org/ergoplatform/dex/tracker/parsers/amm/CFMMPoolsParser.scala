package org.ergoplatform.dex.tracker.parsers.amm

import org.ergoplatform.dex.domain.amm.CFMMPool
import org.ergoplatform.dex.domain.amm.state.OnChain
import org.ergoplatform.dex.protocol.amm.AMMType.{CFMMFamily, T2TCFMM}
import org.ergoplatform.ergo.models.Output

trait CFMMPoolsParser[CF <: CFMMFamily] {

  def pool(box: Output): Option[OnChain[CFMMPool]]
}

object CFMMPoolsParser {
  implicit val t2tCfmmPoolParser: CFMMPoolsParser[T2TCFMM] = T2TCfmmPoolsParser
}
