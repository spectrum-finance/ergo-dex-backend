package org.ergoplatform.dex.protocol.amm

import org.ergoplatform.ergo.ErgoTreeTemplate

object constants {

  def reservedErgoTrees: List[ErgoTreeTemplate] = List(
    N2TCFMMTemplates.deposit,
    N2TCFMMTemplates.redeem,
    N2TCFMMTemplates.swapBuyV0,
    N2TCFMMTemplates.swapBuy,
    N2TCFMMTemplates.swapSellV0,
    N2TCFMMTemplates.swapSell,
    N2TCFMMTemplates.depositV0,
    N2TCFMMTemplates.redeemV0,
    T2TCFMMTemplates.swap,
    T2TCFMMTemplates.swapV0,
    T2TCFMMTemplates.redeem,
    T2TCFMMTemplates.redeemV0,
    T2TCFMMTemplates.deposit,
    T2TCFMMTemplates.depositV0,
    ergoBaseOutput
  )

  def ergoBaseOutput: ErgoTreeTemplate =
    ErgoTreeTemplate.unsafeFromString(
      "1005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192" +
        "a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a57304"
    )

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
