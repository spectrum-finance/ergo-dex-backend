package org.ergoplatform.dex.tracker.parsers.amm

import org.ergoplatform.dex.domain.amm.CFMMPool
import org.ergoplatform.dex.domain.amm.state.Confirmed
import org.ergoplatform.dex.protocol.amm.AMMType.{CFMMType, T2T_CFMM}
import org.ergoplatform.ergo.models.Output

trait CFMMPoolsParser[CF <: CFMMType] {

  def pool(box: Output): Option[Confirmed[CFMMPool]]
}

object CFMMPoolsParser {
  implicit val t2tCfmmPoolParser: CFMMPoolsParser[T2T_CFMM] = T2TCFMMPoolsParser
}
