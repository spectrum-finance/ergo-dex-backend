package org.ergoplatform.dex.protocol.amm

import org.ergoplatform.ergo.ErgoTreeTemplate

object constants {

  val reservedErgoTrees: List[ErgoTreeTemplate] = List(
    N2TCFMMTemplates.depositLatest,
    N2TCFMMTemplates.redeemLatest,
    N2TCFMMTemplates.swapBuyV0,
    N2TCFMMTemplates.swapBuyLatest,
    N2TCFMMTemplates.swapSellV0,
    N2TCFMMTemplates.swapSellLatest,
    N2TCFMMTemplates.depositV1,
    N2TCFMMTemplates.redeemV0,
    N2TCFMMTemplates.swapBuyMultiAddress,
    N2TCFMMTemplates.swapSellMultiAddress,
    T2TCFMMTemplates.swapLatest,
    T2TCFMMTemplates.swapMultiAddress,
    T2TCFMMTemplates.swapV0,
    T2TCFMMTemplates.redeemLatest,
    T2TCFMMTemplates.redeemV0,
    T2TCFMMTemplates.depositLatest,
    T2TCFMMTemplates.depositV1,
    T2TCFMMTemplates.depositV0,
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
