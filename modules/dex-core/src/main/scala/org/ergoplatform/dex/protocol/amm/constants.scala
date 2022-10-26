package org.ergoplatform.dex.protocol.amm

import org.ergoplatform.ergo.ErgoTreeTemplate

object constants {

  val reservedErgoTrees: List[ErgoTreeTemplate] = List(
    N2TCFMMTemplates.depositV2,
    N2TCFMMTemplates.redeem,
    N2TCFMMTemplates.swapBuyV0,
    N2TCFMMTemplates.swapBuy,
    N2TCFMMTemplates.swapSellV0,
    N2TCFMMTemplates.swapSell,
    N2TCFMMTemplates.depositV1,
    N2TCFMMTemplates.redeemV0,
    T2TCFMMTemplates.swap,
    T2TCFMMTemplates.swapV0,
    T2TCFMMTemplates.redeem,
    T2TCFMMTemplates.redeemV0,
    T2TCFMMTemplates.depositV2,
    T2TCFMMTemplates.depositV1,
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
