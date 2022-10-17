package org.ergoplatform.dex.tracker.parsers.amm

import org.ergoplatform.dex.domain.amm.CFMMPool
import org.ergoplatform.dex.protocol.amm.AMMType.{CFMMType, N2T_CFMM, T2T_CFMM}
import org.ergoplatform.ergo.domain.Output

trait CFMMPoolsParser[+CT <: CFMMType] {

  def pool(box: Output): Option[CFMMPool]
}

object CFMMPoolsParser {

  def apply[CT <: CFMMType](implicit ev: CFMMPoolsParser[CT]): CFMMPoolsParser[CT] = ev

  implicit val t2tCfmmPoolParser: CFMMPoolsParser[T2T_CFMM] = T2TCFMMPoolsParser
  implicit val n2tCfmmPoolParser: CFMMPoolsParser[N2T_CFMM] = N2TCFMMPoolsParser
}
