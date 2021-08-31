package org.ergoplatform.dex.sources

object n2tContracts {

  val deposit: String =
    """
      |{
      |    val InitiallyLockedLP = 0x7fffffffffffffffL
      |
      |    val poolIn = INPUTS(0)
      |
      |    val validDeposit =
      |        if (INPUTS.size == 2 && poolIn.tokens.size == 3) {
      |            val selfY = SELF.tokens(0)
      |
      |            val validPoolIn = poolIn.tokens(0)._1 == PoolNFT
      |
      |            val poolLP          = poolIn.tokens(1)
      |            val reservesXAmount = poolIn.value
      |            val reservesY       = poolIn.tokens(2)
      |
      |            val supplyLP = InitiallyLockedLP - poolLP._2
      |
      |            val _selfX = SelfX
      |
      |            val minimalReward = min(
      |                _selfX.toBigInt * supplyLP / reservesXAmount,
      |                selfY._2.toBigInt * supplyLP / reservesY._2
      |            )
      |
      |            val rewardOut = OUTPUTS(1)
      |            val rewardLP  = rewardOut.tokens(0)
      |
      |            validPoolIn &&
      |            rewardOut.propositionBytes == Pk.propBytes &&
      |            rewardOut.value >= SELF.value - DexFee - SelfX &&
      |            rewardLP._1 == poolLP._1 &&
      |            rewardLP._2 >= minimalReward
      |        } else false
      |
      |    sigmaProp(Pk || validDeposit)
      |}
      |""".stripMargin

  val redeem =
    """
      |{
      |    val InitiallyLockedLP = 0x7fffffffffffffffL
      |
      |    val poolIn = INPUTS(0)
      |
      |    val validRedeem =
      |        if (INPUTS.size == 2 && poolIn.tokens.size == 3) {
      |            val selfLP = SELF.tokens(0)
      |
      |            val validPoolIn = poolIn.tokens(0)._1 == PoolNFT
      |
      |            val poolLP          = poolIn.tokens(1)
      |            val reservesXAmount = poolIn.value
      |            val reservesY       = poolIn.tokens(2)
      |
      |            val supplyLP = InitiallyLockedLP - poolLP._2
      |
      |            val minReturnX = selfLP._2.toBigInt * reservesXAmount / supplyLP
      |            val minReturnY = selfLP._2.toBigInt * reservesY._2 / supplyLP
      |
      |            val returnOut = OUTPUTS(1)
      |
      |            val returnXAmount = returnOut.value - SELF.value + DexFee
      |            val returnY       = returnOut.tokens(0)
      |
      |            validPoolIn &&
      |            returnOut.propositionBytes == Pk.propBytes &&
      |            returnY._1 == reservesY._1 && // token id matches
      |            returnXAmount >= minReturnX &&
      |            returnY._2 >= minReturnY
      |        } else false
      |
      |    sigmaProp(Pk || validRedeem)
      |}
      |""".stripMargin

  val swapSell: String =
    """
      |{   // ERG -> Token
      |    val FeeDenom            = 1000
      |    val FeeNum              = 996
      |    val DexFeePerTokenNum   = 2L
      |    val DexFeePerTokenDenom = 10L
      |    val MinQuoteAmount      = 800L
      |    val BaseAmount          = 1200L
      |
      |    val poolIn = INPUTS(0)
      |
      |    val validTrade =
      |        if (INPUTS.size == 2 && poolIn.tokens.size == 3) {
      |            val poolNFT = poolIn.tokens(0)._1
      |
      |            val poolY = poolIn.tokens(2)
      |
      |            val poolReservesX = poolIn.value.toBigInt
      |            val poolReservesY = poolY._2.toBigInt
      |            val validPoolIn   = poolNFT == PoolNFT
      |
      |            val rewardBox = OUTPUTS(1)
      |
      |            val quoteAsset  = rewardBox.tokens(0)
      |            val quoteAmount = quoteAsset._2.toBigInt
      |
      |            val fairDexFee = rewardBox.value >= SELF.value - quoteAmount * DexFeePerTokenNum / DexFeePerTokenDenom - BaseAmount
      |
      |            val relaxedOutput = quoteAmount + 1 // handle rounding loss
      |            val fairPrice     = poolReservesY * BaseAmount * FeeNum <= relaxedOutput * (poolReservesX * FeeDenom + BaseAmount * FeeNum)
      |
      |            validPoolIn &&
      |            rewardBox.propositionBytes == Pk.propBytes &&
      |            quoteAsset._1 == QuoteId &&
      |            quoteAmount >= MinQuoteAmount &&
      |            fairDexFee &&
      |            fairPrice
      |        } else false
      |
      |    sigmaProp(Pk || validTrade)
      |}
      |""".stripMargin

  val swapBuy: String =
    """
      |{   // Token -> ERG
      |    val FeeDenom            = 1000
      |    val FeeNum              = 996
      |    val DexFeePerTokenNum   = 1L
      |    val DexFeePerTokenDenom = 10L
      |    val MinQuoteAmount      = 800L
      |
      |    val poolIn = INPUTS(0)
      |
      |    val validTrade =
      |        if (INPUTS.size == 2 && poolIn.tokens.size == 3) {
      |            val baseAmount = SELF.tokens(0)._2
      |
      |            val poolNFT = poolIn.tokens(0)._1  // first token id is NFT
      |
      |            val poolReservesX = poolIn.value.toBigInt   // nanoErgs is X asset amount
      |            val poolReservesY = poolIn.tokens(2)._2.toBigInt // third token amount is Y asset amount
      |
      |            val validPoolIn = poolNFT == PoolNFT
      |
      |            val rewardBox = OUTPUTS(1)
      |
      |            val deltaNErgs    = rewardBox.value - SELF.value // this is quoteAmount - fee
      |            val quoteAmount   = deltaNErgs.toBigInt * DexFeePerTokenDenom / (DexFeePerTokenDenom - DexFeePerTokenNum)
      |            val relaxedOutput = quoteAmount + 1 // handle rounding loss
      |            val fairPrice     = poolReservesX * baseAmount * FeeNum <= relaxedOutput * (poolReservesY * FeeDenom + baseAmount * FeeNum)
      |
      |            validPoolIn &&
      |            rewardBox.propositionBytes == Pk.propBytes &&
      |            quoteAmount >= MinQuoteAmount &&
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
      |    val FeeDenom = 1000
      |    val MinStorageRent = 10000000L  // this many number of nanoErgs are going to be permanently locked
      |
      |    val poolNFT0    = SELF.tokens(0)
      |    val reservedLP0 = SELF.tokens(1)
      |    val tokenY0     = SELF.tokens(2)
      |
      |    val successor = OUTPUTS(0)
      |
      |    val feeNum0 = SELF.R4[Int].get
      |    val feeNum1 = successor.R4[Int].get
      |
      |    val poolNFT1    = successor.tokens(0)
      |    val reservedLP1 = successor.tokens(1)
      |    val tokenY1     = successor.tokens(2)
      |
      |    val validSuccessorScript = successor.propositionBytes == SELF.propositionBytes
      |    val preservedFeeConfig   = feeNum1 == feeNum0
      |
      |    val preservedPoolNFT     = poolNFT1 == poolNFT0
      |    val validLP              = reservedLP1._1 == reservedLP0._1
      |    val validY               = tokenY1._1 == tokenY0._1
      |    // since tokens can be repeated, we ensure for sanity that there are no more tokens
      |    val noMoreTokens         = successor.tokens.size == 3
      |
      |    val validStorageRent     = successor.value > MinStorageRent
      |
      |    val supplyLP0 = InitiallyLockedLP - reservedLP0._2
      |    val supplyLP1 = InitiallyLockedLP - reservedLP1._2
      |
      |    val reservesX0 = SELF.value
      |    val reservesY0 = tokenY0._2
      |    val reservesX1 = successor.value
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
      |        // note: _deltaSupplyLP, deltaReservesX and deltaReservesY are negative
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
      |        preservedPoolNFT &&
      |        validLP &&
      |        validY &&
      |        noMoreTokens &&
      |        validAction &&
      |        validStorageRent
      |    )
      |}
      |""".stripMargin
}
