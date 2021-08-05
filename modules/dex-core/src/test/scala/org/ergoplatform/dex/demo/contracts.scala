package org.ergoplatform.dex.demo

object contracts {

  val deposit: String =
    """
      |{
      |    val InitiallyLockedLP = 0x7fffffffffffffffL
      |
      |    val selfX = SELF.tokens(0)
      |    val selfY = SELF.tokens(1)
      |
      |    val poolIn = INPUTS(0)
      |
      |    val validDeposit =
      |        if (INPUTS.size == 2 && poolIn.tokens.size == 4) {
      |            val validPoolIn  = poolIn.tokens(0) == (PoolNFT, 1L)
      |
      |            val poolLP    = poolIn.tokens(1)
      |            val reservesX = poolIn.tokens(2)
      |            val reservesY = poolIn.tokens(3)
      |
      |            val supplyLP = InitiallyLockedLP - poolLP._2
      |
      |            val minimalReward = min(
      |                selfX._2.toBigInt * supplyLP / reservesX._2,
      |                selfY._2.toBigInt * supplyLP / reservesY._2
      |            )
      |
      |            val rewardOut = OUTPUTS(1)
      |            val rewardLP  = rewardOut.tokens(0)
      |
      |            validPoolIn &&
      |            rewardOut.propositionBytes == Pk.propBytes &&
      |            rewardOut.value >= SELF.value - DexFee &&
      |            rewardLP._1 == poolLP._1 &&
      |            rewardLP._2 >= minimalReward
      |        } else false
      |
      |    sigmaProp(Pk || validDeposit)
      |}
      |""".stripMargin

  val redeem: String =
    """
      |{
      |    val InitiallyLockedLP = 0x7fffffffffffffffL
      |
      |    val selfLP = SELF.tokens(0)
      |
      |    val poolIn = INPUTS(0)
      |
      |    val validRedeem =
      |        if (INPUTS.size == 2 && poolIn.tokens.size == 4) {
      |            val validPoolIn = poolIn.tokens(0) == (PoolNFT, 1L)
      |
      |            val poolLP    = poolIn.tokens(1)
      |            val reservesX = poolIn.tokens(2)
      |            val reservesY = poolIn.tokens(3)
      |
      |            val supplyLP = InitiallyLockedLP - poolLP._2
      |
      |            val minReturnX = selfLP._2.toBigInt * reservesX._2 / supplyLP
      |            val minReturnY = selfLP._2.toBigInt * reservesY._2 / supplyLP
      |
      |            val returnOut = OUTPUTS(1)
      |
      |            val returnX = returnOut.tokens(0)
      |            val returnY = returnOut.tokens(1)
      |
      |            validPoolIn &&
      |            returnOut.propositionBytes == Pk.propBytes &&
      |            returnOut.value >= SELF.value - DexFee &&
      |            returnX._1 == reservesX._1 &&
      |            returnY._1 == reservesY._1 &&
      |            returnX._2 >= minReturnX &&
      |            returnY._2 >= minReturnY
      |        } else false
      |
      |    sigmaProp(Pk || validRedeem)
      |}
      |""".stripMargin

  val swap: String =
    """
      |{
      |    val FeeDenom = 1000
      |    val FeeNum   = 996
      |    val DexFeePerTokenNum   = 1L
      |    val DexFeePerTokenDenom = 10L
      |
      |    val poolIn = INPUTS(0)
      |
      |    val validTrade =
      |        if (INPUTS.size == 2 && poolIn.tokens.size == 4) {
      |            val base       = SELF.tokens(0)
      |            val baseId     = base._1
      |            val baseAmount = base._2.toBigInt
      |
      |            val poolNFT    = poolIn.tokens(0)._1
      |            val poolAssetX = poolIn.tokens(2)
      |            val poolAssetY = poolIn.tokens(3)
      |
      |            val validPoolIn = poolNFT == PoolNFT
      |
      |            val rewardBox     = OUTPUTS(1)
      |            val quoteAsset    = rewardBox.tokens(0)
      |            val quoteAmount   = quoteAsset._2.toBigInt
      |            val fairDexFee    = rewardBox.value >= SELF.value - quoteAmount * DexFeePerTokenNum / DexFeePerTokenDenom
      |            val relaxedOutput = quoteAmount + 1L // handle rounding loss
      |            val poolX         = poolAssetX._2.toBigInt
      |            val poolY         = poolAssetY._2.toBigInt
      |            val fairPrice     =
      |                if (poolAssetX._1 == QuoteId)
      |                    poolX * baseAmount * FeeNum <= relaxedOutput * (poolY * FeeDenom + baseAmount * FeeNum)
      |                else
      |                    poolY * baseAmount * FeeNum <= relaxedOutput * (poolX * FeeDenom + baseAmount * FeeNum)
      |
      |            validPoolIn &&
      |            rewardBox.propositionBytes == Pk.propBytes &&
      |            quoteAsset._1 == QuoteId &&
      |            quoteAsset._2 >= MinQuoteAmount &&
      |            fairDexFee &&
      |            fairPrice
      |        } else false
      |
      |    sigmaProp(Pk || validTrade)
      |}
      |""".stripMargin

  val pool: String =
    """
      |{
      |    val InitiallyLockedLP = 0x7fffffffffffffffL
      |    val FeeDenom          = 1000
      |
      |    val ergs0       = SELF.value
      |    val poolNFT0    = SELF.tokens(0)
      |    val reservedLP0 = SELF.tokens(1)
      |    val tokenX0     = SELF.tokens(2)
      |    val tokenY0     = SELF.tokens(3)
      |
      |    val successor = OUTPUTS(0)
      |
      |    val feeNum0 = SELF.R4[Int].get
      |    val feeNum1 = successor.R4[Int].get
      |
      |    val ergs1       = successor.value
      |    val poolNFT1    = successor.tokens(0)
      |    val reservedLP1 = successor.tokens(1)
      |    val tokenX1     = successor.tokens(2)
      |    val tokenY1     = successor.tokens(3)
      |
      |    val validSuccessorScript = successor.propositionBytes == SELF.propositionBytes
      |    val preservedFeeConfig   = feeNum1 == feeNum0
      |    val preservedErgs        = ergs1 >= ergs0
      |    val preservedPoolNFT     = poolNFT1 == poolNFT0
      |    val validLP              = reservedLP1._1 == reservedLP0._1
      |    val validPair            = tokenX1._1 == tokenX0._1 && tokenY1._1 == tokenY0._1
      |
      |    val supplyLP0 = InitiallyLockedLP - reservedLP0._2
      |    val supplyLP1 = InitiallyLockedLP - reservedLP1._2
      |
      |    val reservesX0 = tokenX0._2
      |    val reservesY0 = tokenY0._2
      |    val reservesX1 = tokenX1._2
      |    val reservesY1 = tokenY1._2
      |
      |    val deltaSupplyLP  = supplyLP1 - supplyLP0
      |    val deltaReservesX = reservesX1 - reservesX0
      |    val deltaReservesY = reservesY1 - reservesY0
      |
      |    val validDepositing = {
      |        val sharesUnlocked = min(
      |            deltaReservesX.toBigInt * supplyLP0 / reservesX0,
      |            deltaReservesY.toBigInt * supplyLP0 / reservesY0
      |        )
      |        deltaSupplyLP <= sharesUnlocked
      |    }
      |
      |    val validRedemption = {
      |        val _deltaSupplyLP = deltaSupplyLP.toBigInt
      |        // note: _deltaSupplyLP and deltaReservesX, deltaReservesY are negative
      |        deltaReservesX.toBigInt * supplyLP0 >= _deltaSupplyLP * reservesX0 && deltaReservesY.toBigInt * supplyLP0 >= _deltaSupplyLP * reservesY0
      |    }
      |
      |    val validSwap =
      |        if (deltaReservesX > 0)
      |            reservesY0.toBigInt * deltaReservesX * feeNum0 >= -deltaReservesY * (reservesX0.toBigInt * FeeDenom + deltaReservesX * feeNum0)
      |        else
      |            reservesX0.toBigInt * deltaReservesY * feeNum0 >= -deltaReservesX * (reservesY0.toBigInt * FeeDenom + deltaReservesY * feeNum0)
      |
      |    val validAction =
      |        if (deltaSupplyLP == 0)
      |            validSwap
      |        else
      |            if (deltaReservesX > 0 && deltaReservesY > 0) validDepositing
      |            else validRedemption
      |
      |    sigmaProp(
      |        validSuccessorScript &&
      |        preservedFeeConfig &&
      |        preservedErgs &&
      |        preservedPoolNFT &&
      |        validLP &&
      |        validPair &&
      |        validAction
      |    )
      |}
      |""".stripMargin
}
