package org.ergoplatform.dex.protocol.amm

trait AMMType

object AMMType {
  trait CFMMFamily extends AMMType
  trait T2TCFMM extends CFMMFamily
  trait N2TCFMM extends CFMMFamily
}
