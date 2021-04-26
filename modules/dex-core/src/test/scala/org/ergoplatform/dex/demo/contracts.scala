package org.ergoplatform.dex.demo

object contracts {

  def swapContract: String =
    """
      |{
      |    val Pk = pk
      |
      |    val PoolScriptHash = poolScriptHash
      |
      |    val DexFeePerToken = dexFeePerToken
      |    val MinQuoteAmount = minQuoteAmount
      |    val QuoteId        = quoteId
      |
      |    val MinerFee = minerFee
      |
      |    val FeeNum   = poolFeeNum
      |    val FeeDenom = 1000
      |
      |    val base       = SELF.tokens(0)
      |    val baseId     = base._1
      |    val baseAmount = base._2
      |
      |    val poolInput  = INPUTS(0)
      |    val poolAssetX = poolInput.tokens(2)
      |    val poolAssetY = poolInput.tokens(3)
      |
      |    val validPoolInput =
      |        blake2b256(poolInput.propositionBytes) == PoolScriptHash &&
      |        (poolAssetX._1 == QuoteId || poolAssetY._1 == QuoteId) &&
      |        (poolAssetX._1 == baseId  || poolAssetY._1 == baseId)
      |
      |    val validTrade =
      |        OUTPUTS.exists { (box: Box) =>
      |            val quoteAsset  = box.tokens(0)
      |            val quoteAmount = quoteAsset._2
      |            val fairDexFee  = box.value >= SELF.value - MinerFee - quoteAmount * DexFeePerToken
      |            val fairPrice   =
      |                if (poolAssetX._1 == QuoteId)
      |                    poolAssetX._2.toBigInt * baseAmount * FeeNum >= quoteAmount * (poolAssetY._2.toBigInt * FeeDenom + baseAmount * FeeNum)
      |                else
      |                    poolAssetY._2.toBigInt * baseAmount * FeeNum >= quoteAmount * (poolAssetX._2.toBigInt * FeeDenom + baseAmount * FeeNum)
      |
      |            box.propositionBytes == Pk.propBytes &&
      |            quoteAsset._1 == QuoteId &&
      |            quoteAsset._2 >= MinQuoteAmount &&
      |            fairDexFee &&
      |            fairPrice
      |        }
      |
      |    sigmaProp(Pk || (validPoolInput && validTrade))
      |}
      |""".stripMargin

  def bootContract: String =
    """
      |{
      |    val InitiallyLockedLP    = 1000000000000000000L
      |
      |    val poolScriptHash  = SELF.R5[Coll[Byte]].get
      |    val desiredSharesLP = SELF.R6[Long].get
      |    val poolFeeConfig   = SELF.R7[Long].get
      |    val minerFeeNErgs   = SELF.R8[Long].get
      |    val initiatorProp   = SELF.R9[Coll[Byte]].get
      |
      |    val selfLP = SELF.tokens(0)
      |    val selfX  = SELF.tokens(1)
      |    val selfY  = SELF.tokens(2)
      |
      |    val tokenIdLP = selfLP._1
      |
      |    // self checks
      |    val validSelfLP            = selfLP._2 == InitiallyLockedLP // Correct amount of LP tokens issued
      |    val validSelfPoolFeeConfig = poolFeeConfig <= 1000L && poolFeeConfig > 750L // Correct pool fee config
      |
      |    val pool           = OUTPUTS(0)
      |    val sharesRewardLP = OUTPUTS(1)
      |
      |    val maybePoolLP  = pool.tokens(1)
      |    val poolAmountLP =
      |        if (maybePoolLP._1 == tokenIdLP) maybePoolLP._2
      |        else 0L
      |
      |    val validPoolContract  = blake2b256(pool.propositionBytes) == poolScriptHash
      |    val validPoolErgAmount = pool.value == SELF.value - sharesRewardLP.value - minerFeeNErgs
      |    val validPoolNFT       = pool.tokens(0) == (SELF.id, 1L)
      |    val validPoolConfig    = pool.R4[Long].get == poolFeeConfig
      |
      |    val validInitialDepositing = {
      |        val tokenX     = pool.tokens(2)
      |        val tokenY     = pool.tokens(3)
      |        val depositedX = tokenX._2
      |        val depositedY = tokenY._2
      |
      |        val validTokens  = tokenX == selfX && tokenY == selfY
      |        val validDeposit = depositedX.toBigInt * depositedY >= desiredSharesLP.toBigInt * desiredSharesLP // S >= sqrt(X_deposited * Y_deposited) Deposits satisfy desired share
      |        val validShares  = poolAmountLP >= (InitiallyLockedLP - desiredSharesLP)                          // valid amount of liquidity shares taken from reserves
      |
      |        validTokens && validDeposit && validShares
      |    }
      |
      |    val validPool = validPoolContract && validPoolErgAmount && validPoolNFT && validInitialDepositing
      |
      |    val validSharesRewardLP =
      |        sharesRewardLP.propositionBytes == initiatorProp &&
      |        sharesRewardLP.tokens(0) == (tokenIdLP, desiredSharesLP)
      |
      |    sigmaProp(validSelfLP && validSelfPoolFeeConfig && validPool && validSharesRewardLP)
      |}
      |""".stripMargin

  def poolContract: String =
    """
      |{
      |    val InitiallyLockedLP = 1000000000000000000L
      |
      |    val feeNum0  = SELF.R4[Long].get
      |    val FeeDenom = 1000
      |
      |    val ergs0       = SELF.value
      |    val poolNFT0    = SELF.tokens(0)
      |    val reservedLP0 = SELF.tokens(1)
      |    val tokenX0     = SELF.tokens(2)
      |    val tokenY0     = SELF.tokens(3)
      |
      |    val successor = OUTPUTS(0)
      |
      |    val feeNum1 = successor.R4[Long].get
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
      |        -deltaSupplyLP <= sharesUnlocked
      |    }
      |
      |    val validRedemption = {
      |        val shareLP = deltaSupplyLP.toBigInt / supplyLP0
      |        // note: shareLP and deltaReservesX, deltaReservesY are negative
      |        deltaReservesX >= shareLP * reservesX0 && deltaReservesY >= shareLP * reservesY0
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
