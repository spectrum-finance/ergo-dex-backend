package org.ergoplatform.dex.protocol.amm

trait AmmContractType

object AmmContractType {
  trait CfmmFamily extends AmmContractType
  trait T2tCfmm extends CfmmFamily
  trait N2tCfmm extends CfmmFamily
}
