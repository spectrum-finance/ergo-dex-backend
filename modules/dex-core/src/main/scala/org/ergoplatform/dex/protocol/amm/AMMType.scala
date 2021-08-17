package org.ergoplatform.dex.protocol.amm

trait AMMType

object AMMType {
  trait CFMMType extends AMMType
  trait T2T_CFMM extends CFMMType
  trait N2T_CFMM extends CFMMType
}
