package org.ergoplatform.dex.protocol.amm

import org.ergoplatform.ergo.ErgoTreeTemplate

object constants {

  val reservedErgoTrees: List[ErgoTreeTemplate] = List(
    N2TCFMMTemplates.depositV3,
    N2TCFMMTemplates.redeemV3,
    N2TCFMMTemplates.swapSellV3,
    N2TCFMMTemplates.swapBuyV3,
    T2TCFMMTemplates.depositV3,
    T2TCFMMTemplates.redeemV3,
    T2TCFMMTemplates.swapV3,
    N2TCFMMTemplates.swapBuyMultiAddressV2,
    N2TCFMMTemplates.swapSellMultiAddressV2,
    T2TCFMMTemplates.swapV2,
    N2TCFMMTemplates.depositV1,
    N2TCFMMTemplates.redeemV1,
    N2TCFMMTemplates.swapBuyV1,
    N2TCFMMTemplates.swapSellV1,
    T2TCFMMTemplates.swapV1,
    T2TCFMMTemplates.redeemV1,
    T2TCFMMTemplates.depositV1,
    N2TCFMMTemplates.depositLegacyV1,
    T2TCFMMTemplates.depositLegacyV1,
    N2TCFMMTemplates.swapBuyLegacyV0,
    N2TCFMMTemplates.swapSellLegacyV0,
    N2TCFMMTemplates.redeemLegacyV0,
    T2TCFMMTemplates.swapLegacyV0,
    T2TCFMMTemplates.depositLegacyV0,
    T2TCFMMTemplates.redeemLegacyV0,
    ergoBaseOutput,
    ErgoTreeTemplate.fromBytes(AMMContracts.N2TCFMMContracts.pool.template),
    ErgoTreeTemplate.fromBytes(AMMContracts.T2TCFMMContracts.pool.template)
  )

  def ergoBaseOutput: ErgoTreeTemplate =
    ErgoTreeTemplate.unsafeFromString("d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a57304")

  object cfmm {

    val TotalEmissionLP = 0x7fffffffffffffffL
    val FeeDenominator  = 1000

    object t2t {
      val IndexNFT = 0
      val IndexLP  = 1
      val IndexX   = 2
      val IndexY   = 3
    }

    object n2t {
      val IndexNFT = 0
      val IndexLP  = 1
      val IndexY   = 2
    }
  }
}
