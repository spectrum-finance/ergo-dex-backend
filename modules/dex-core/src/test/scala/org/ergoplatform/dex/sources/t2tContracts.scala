package org.ergoplatform.dex.sources

object t2tContracts {

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
      |            val reservesXAmount = reservesX._2
      |            val reservesYAmount = reservesY._2
      |
      |            val supplyLP = InitiallyLockedLP - poolLP._2
      |
      |            val minByX = selfX._2.toBigInt * supplyLP / reservesXAmount
      |            val minByY = selfY._2.toBigInt * supplyLP / reservesYAmount
      |
      |            val minimalReward = min(minByX, minByY)
      |
      |            val rewardOut = OUTPUTS(1)
      |            val rewardLP  = rewardOut.tokens(0)
      |
      |            val validErgChange = rewardOut.value >= SELF.value - DexFee
      |
      |            val validTokenChange =
      |                if (minByX < minByY && rewardOut.tokens.size == 2) {
      |                    val diff = minByY - minByX
      |                    val excessY = diff * reservesYAmount / supplyLP
      |
      |                    val changeY = rewardOut.tokens(1)
      |
      |                    changeY._1 == reservesY._1 &&
      |                    changeY._2 >= excessY
      |                } else if (minByX > minByY && rewardOut.tokens.size == 2) {
      |                    val diff = minByX - minByY
      |                    val excessX = diff * reservesXAmount / supplyLP
      |
      |                    val changeX = rewardOut.tokens(1)
      |
      |                    changeX._1 == reservesX._1 &&
      |                    changeX._2 >= excessX
      |                } else if (minByX == minByY) {
      |                    true
      |                } else {
      |                    false
      |                }
      |
      |            val validMinerFee = OUTPUTS.map { (o: Box) =>
      |                if (o.propositionBytes == MinerPropBytes) o.value else 0L
      |            }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee
      |
      |            validPoolIn &&
      |            rewardOut.propositionBytes == Pk.propBytes &&
      |            validErgChange &&
      |            validTokenChange &&
      |            rewardLP._1 == poolLP._1 &&
      |            rewardLP._2 >= minimalReward &&
      |            validMinerFee
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
      |            val validMinerFee = OUTPUTS.map { (o: Box) =>
      |                if (o.propositionBytes == MinerPropBytes) o.value else 0L
      |            }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee
      |
      |            validPoolIn &&
      |            returnOut.propositionBytes == Pk.propBytes &&
      |            returnOut.value >= SELF.value - DexFee &&
      |            returnX._1 == reservesX._1 &&
      |            returnY._1 == reservesY._1 &&
      |            returnX._2 >= minReturnX &&
      |            returnY._2 >= minReturnY &&
      |            validMinerFee
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
      |    val MinQuoteAmount      = 800L
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
      |            val dexFee        = quoteAmount * DexFeePerTokenNum / DexFeePerTokenDenom
      |            val fairDexFee    = rewardBox.value >= SELF.value - dexFee
      |            val relaxedOutput = quoteAmount + 1L // handle rounding loss
      |            val poolX         = poolAssetX._2.toBigInt
      |            val poolY         = poolAssetY._2.toBigInt
      |            val fairPrice     =
      |                if (poolAssetX._1 == QuoteId)
      |                    poolX * baseAmount * FeeNum <= relaxedOutput * (poolY * FeeDenom + baseAmount * FeeNum)
      |                else
      |                    poolY * baseAmount * FeeNum <= relaxedOutput * (poolX * FeeDenom + baseAmount * FeeNum)
      |
      |            val validMinerFee = OUTPUTS.map { (o: Box) =>
      |                if (o.propositionBytes == MinerPropBytes) o.value else 0L
      |            }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee
      |
      |            validPoolIn &&
      |            rewardBox.propositionBytes == Pk.propBytes &&
      |            quoteAsset._1 == QuoteId &&
      |            quoteAsset._2 >= MinQuoteAmount &&
      |            fairDexFee &&
      |            fairPrice &&
      |            validMinerFee
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

  val depositV3: String =
    s"""
       |{ // ===== Contract Information ===== //
       |  // Name: Deposit
       |  // Description: Contract that validates user's deposit into the CFMM t2t Pool.
       |  //
       |  // Constants:
       |  //
       |  // {1}  -> RefundProp[ProveDlog]
       |  // {8}  -> SelfXAmount[Long] // SELF.tokens(0)._2 - ExFee if X is SPF else SELF.tokens(0)._2
       |  // {10} -> SelfYAmount[Long] // SELF.tokens(1)._2 - ExFee if Y is SPF else SELF.tokens(1)._2
       |  // {13} -> PoolNFT[Coll[Byte]]
       |  // {14} -> RedeemerPropBytes[Coll[Byte]]
       |  // {21} -> MinerPropBytes[Coll[Byte]]
       |  // {24} -> MaxMinerFee[Long]
       |  //
       |  // ErgoTree: 19dd041a040008cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d040404080402040205feffffffffffffffff01040405a01f040605f02e040004000e2002020202020202020202020202020202020202020202020202020202020202020e2001010101010101010101010101010101010101010101010101010101010101010404040204040402010101000e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100d801d601b2a4730000eb027301d195ed92b1a4730293b1db630872017303d80cd602db63087201d603b2a5730400d604b27202730500d6057e9973068c72040206d606b27202730700d6077e8c72060206d6089d9c7e73080672057207d609b27202730900d60a7e8c72090206d60b9d9c7e730a067205720ad60cdb63087203d60db2720c730b00edededededed938cb27202730c0001730d93c27203730e92c17203c1a795ed8f7208720b93b1720c730fd801d60eb2720c731000ed938c720e018c720901927e8c720e02069d9c99720b7208720a720595ed917208720b93b1720c7311d801d60eb2720c731200ed938c720e018c720601927e8c720e02069d9c997208720b7207720595937208720b73137314938c720d018c720401927e8c720d0206a17208720b90b0ada5d9010e639593c2720e7315c1720e73167317d9010e599a8c720e018c720e0273187319
       |  //
       |  // ErgoTreeTemplate: d801d601b2a4730000eb027301d195ed92b1a4730293b1db630872017303d80cd602db63087201d603b2a5730400d604b27202730500d6057e9973068c72040206d606b27202730700d6077e8c72060206d6089d9c7e73080672057207d609b27202730900d60a7e8c72090206d60b9d9c7e730a067205720ad60cdb63087203d60db2720c730b00edededededed938cb27202730c0001730d93c27203730e92c17203c1a795ed8f7208720b93b1720c730fd801d60eb2720c731000ed938c720e018c720901927e8c720e02069d9c99720b7208720a720595ed917208720b93b1720c7311d801d60eb2720c731200ed938c720e018c720601927e8c720e02069d9c997208720b7207720595937208720b73137314938c720d018c720401927e8c720d0206a17208720b90b0ada5d9010e639593c2720e7315c1720e73167317d9010e599a8c720e018c720e0273187319
       |
       |  val InitiallyLockedLP = 0x7fffffffffffffffL
       |  val selfXAmount       = SelfXAmount
       |  val selfYAmount       = SelfYAmount
       |
       |  val poolIn = INPUTS(0)
       |
       |  // Validations
       |  // 1.
       |  val validDeposit =
       |    if (INPUTS.size >= 2 && poolIn.tokens.size == 4) {
       |      // 1.1.
       |      val validPoolIn = poolIn.tokens(0)._1 == PoolNFT
       |
       |      val poolLP    = poolIn.tokens(1)
       |      val reservesX = poolIn.tokens(2)
       |      val reservesY = poolIn.tokens(3)
       |
       |      val reservesXAmount = reservesX._2
       |      val reservesYAmount = reservesY._2
       |
       |      val supplyLP = InitiallyLockedLP - poolLP._2
       |
       |      val minByX = selfXAmount.toBigInt * supplyLP / reservesXAmount
       |      val minByY = selfYAmount.toBigInt * supplyLP / reservesYAmount
       |
       |      val minimalReward = min(minByX, minByY)
       |
       |      val rewardOut = OUTPUTS(1)
       |      val rewardLP  = rewardOut.tokens(0)
       |      // 1.2.
       |      val validErgChange = rewardOut.value >= SELF.value
       |      // 1.3.
       |      val validTokenChange =
       |        if (minByX < minByY && rewardOut.tokens.size == 2) {
       |          val diff    = minByY - minByX
       |          val excessY = diff * reservesYAmount / supplyLP
       |
       |          val changeY = rewardOut.tokens(1)
       |
       |          changeY._1 == reservesY._1 &&
       |          changeY._2 >= excessY
       |
       |        } else if (minByX > minByY && rewardOut.tokens.size == 2) {
       |          val diff    = minByX - minByY
       |          val excessX = diff * reservesXAmount / supplyLP
       |
       |          val changeX = rewardOut.tokens(1)
       |
       |          changeX._1 == reservesX._1 &&
       |          changeX._2 >= excessX
       |
       |        } else if (minByX == minByY) {
       |          true
       |
       |        } else {
       |          false
       |        }
       |
       |      // 1.4.
       |      val validMinerFee = OUTPUTS.map { (o: Box) =>
       |        if (o.propositionBytes == MinerPropBytes) o.value else 0L
       |      }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee
       |
       |      validPoolIn &&
       |      rewardOut.propositionBytes == RedeemerPropBytes &&
       |      validErgChange &&
       |      validTokenChange &&
       |      rewardLP._1 == poolLP._1 &&
       |      rewardLP._2 >= minimalReward &&
       |      validMinerFee
       |
       |    } else false
       |
       |  sigmaProp(RefundProp || validDeposit)
       |}"""
      .stripMargin

  val redeemV3 =
    """
      |{ // ===== Contract Information ===== //
      |  // Name: Redeem
      |  // Description: Contract that validates user's redeem from the CFMM t2t Pool.
      |  //
      |  // Constants:
      |  //
      |  // {1}  -> RefundProp[ProveDlog]
      |  // {13} -> PoolNFT[Coll[Byte]]
      |  // {14} -> RedeemerPropBytes[Coll[Byte]]
      |  // {15} -> MinerPropBytes[Coll[Byte]]
      |  // {18} -> MaxMinerFee[Long]
      |  //
      |  // ErgoTree: 19e60314040008cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d0404040804020400040404020406040005feffffffffffffffff01040204000e2002020202020202020202020202020202020202020202020202020202020202020e2001010101010101010101010101010101010101010101010101010101010101010e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100d801d601b2a4730000eb027301d195ed92b1a4730293b1db630872017303d809d602db63087201d603b2a5730400d604db63087203d605b27204730500d606b27202730600d607b27204730700d608b27202730800d6097e8cb2db6308a77309000206d60a7e99730a8cb27202730b000206edededededed938cb27202730c0001730d93c27203730e938c7205018c720601938c7207018c720801927e8c720502069d9c72097e8c72060206720a927e8c720702069d9c72097e8c72080206720a90b0ada5d9010b639593c2720b730fc1720b73107311d9010b599a8c720b018c720b0273127313
      |  //
      |  // ErgoTreeTemplate: d801d601b2a4730000eb027301d195ed92b1a4730293b1db630872017303d809d602db63087201d603b2a5730400d604db63087203d605b27204730500d606b27202730600d607b27204730700d608b27202730800d6097e8cb2db6308a77309000206d60a7e99730a8cb27202730b000206edededededed938cb27202730c0001730d93c27203730e938c7205018c720601938c7207018c720801927e8c720502069d9c72097e8c72060206720a927e8c720702069d9c72097e8c72080206720a90b0ada5d9010b639593c2720b730fc1720b73107311d9010b599a8c720b018c720b0273127313
      |
      |  val InitiallyLockedLP = 0x7fffffffffffffffL
      |
      |  val selfLP = SELF.tokens(0)
      |
      |  val poolIn = INPUTS(0)
      |
      |  // Validations
      |  // 1.
      |  val validRedeem =
      |    if (INPUTS.size >= 2 && poolIn.tokens.size == 4) {
      |      val validPoolIn = poolIn.tokens(0)._1 == PoolNFT
      |
      |      val poolLP    = poolIn.tokens(1)
      |      val reservesX = poolIn.tokens(2)
      |      val reservesY = poolIn.tokens(3)
      |
      |      val supplyLP = InitiallyLockedLP - poolLP._2
      |
      |      val selfLPAmount = selfLP._2.toBigInt
      |      val minReturnX   = selfLPAmount * reservesX._2 / supplyLP
      |      val minReturnY   = selfLPAmount * reservesY._2 / supplyLP
      |
      |      val returnOut = OUTPUTS(1)
      |
      |      val returnX = returnOut.tokens(0)
      |      val returnY = returnOut.tokens(1)
      |
      |      val validMinerFee = OUTPUTS.map { (o: Box) =>
      |        if (o.propositionBytes == MinerPropBytes) o.value else 0L
      |      }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee
      |
      |      validPoolIn &&
      |      returnOut.propositionBytes == RedeemerPropBytes &&
      |      returnX._1 == reservesX._1 &&
      |      returnY._1 == reservesY._1 &&
      |      returnX._2 >= minReturnX &&
      |      returnY._2 >= minReturnY &&
      |      validMinerFee
      |
      |    } else false
      |
      |  sigmaProp(RefundProp || validRedeem)
      |}
      |""".stripMargin

  val swapV2: String =
    """
      |{
      |    val FeeDenom = 1000
      |    val FeeNum   = 996
      |    val DexFeePerTokenNum   = 10000000L
      |    val DexFeePerTokenDenom = 5L
      |    val MinQuoteAmount      = 3L
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
      |            val dexFee        = quoteAmount * DexFeePerTokenNum / DexFeePerTokenDenom
      |            val fairDexFee    = rewardBox.value >= SELF.value - dexFee
      |            val relaxedOutput = quoteAmount + 1L // handle rounding loss
      |            val poolX         = poolAssetX._2.toBigInt
      |            val poolY         = poolAssetY._2.toBigInt
      |            val fairPrice     =
      |                if (poolAssetX._1 == QuoteId)
      |                    poolX * baseAmount * FeeNum <= relaxedOutput * (poolY * FeeDenom + baseAmount * FeeNum)
      |                else
      |                    poolY * baseAmount * FeeNum <= relaxedOutput * (poolX * FeeDenom + baseAmount * FeeNum)
      |
      |            val validMinerFee = OUTPUTS.map { (o: Box) =>
      |                if (o.propositionBytes == MinerPropBytes) o.value else 0L
      |            }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee
      |
      |            validPoolIn &&
      |            rewardBox.propositionBytes == RedeemerPropBytes &&
      |            quoteAsset._1 == QuoteId &&
      |            quoteAsset._2 >= MinQuoteAmount &&
      |            fairDexFee &&
      |            fairPrice &&
      |            validMinerFee
      |        } else false
      |
      |    sigmaProp(RefundProp || validTrade)
      |}
      |""".stripMargin

  val swapV3: String =
    s"""
       |{ // ===== Contract Information ===== //
       |  // Name: Swap
       |  // Description: Contract that validates user's swap from token to token in the CFMM t2t Pool.
       |  //
       |  // Constants:
       |  //
       |  // {1} -> QuoteId[Coll[Byte]]
       |  // {2} -> MaxExFee[Long]
       |  // {3} -> ExFeePerTokenDenom[Long]
       |  // {4} -> BaseAmount[Long]
       |  // {5} -> FeeNum[Int]
       |  // {6} -> FeeDenom[Int]
       |  // {7} -> RefundProp[ProveDlog]
       |  // {12} -> SpectrumIsQuote[Boolean]
       |  // {18} -> PoolNFT[Coll[Byte]]
       |  // {19} -> RedeemerPropBytes[Coll[Byte]]
       |  // {20} -> MinQuoteAmount[Long]
       |  // {23} -> ExFeePerTokenNum[Long]
       |  // {26} -> SpectrumId[Coll[Byte]]
       |  // {28} -> MinerPropBytes[Coll[Byte]]
       |  // {31} -> MaxMinerFee[Long]
       |  //
       |  // ErgoTree: 19b7052104000e20040404040404040404040404040404040404040404040404040404040404040405f01505c80105e01204c80f04d00f08cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d04040408040204000100059c010404040606010104000e2002020202020202020202020202020202020202020202020202020202020202020e20010101010101010101010101010101010101010101010101010101010101010105c00c01000101052c06010004020e20030303030303030303030303030303030303030303030303030303030303030301010e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100d806d601b2a4730000d6027301d6037302d6047303d6059c73047e730505d6067306eb027307d195ed92b1a4730893b1db630872017309d80ad607db63087201d608b2a5730a00d609db63087208d60ab27209730b00d60b8c720a02d60c95730c9d9c997e720b067e7203067e7204067e730d067e720b06d60db27207730e00d60e7e8c720d0206d60f7e8cb27207730f000206d6109a720c7310ededededededed938cb2720773110001731293c272087313938c720a01720292720b731492c17208c1a79573157316d801d611997e7203069d9c720c7e7317067e720406959172117318d801d612b27209731900ed938c721201731a927e8c721202067211731b95938c720d017202909c720e7e7205069c72109a9c720f7e7206067e720506909c720f7e7205069c72109a9c720e7e7206067e72050690b0ada5d90111639593c27211731cc17211731d731ed90111599a8c7211018c721102731f7320
       |  //
       |  // ErgoTreeTemplate: d806d601b2a4730000d6027301d6037302d6047303d6059c73047e730505d6067306eb027307d195ed92b1a4730893b1db630872017309d80ad607db63087201d608b2a5730a00d609db63087208d60ab27209730b00d60b8c720a02d60c95730c9d9c997e720b067e7203067e7204067e730d067e720b06d60db27207730e00d60e7e8c720d0206d60f7e8cb27207730f000206d6109a720c7310ededededededed938cb2720773110001731293c272087313938c720a01720292720b731492c17208c1a79573157316d801d611997e7203069d9c720c7e7317067e720406959172117318d801d612b27209731900ed938c721201731a927e8c721202067211731b95938c720d017202909c720e7e7205069c72109a9c720f7e7206067e720506909c720f7e7205069c72109a9c720e7e7206067e72050690b0ada5d90111639593c27211731cc17211731d731ed90111599a8c7211018c721102731f7320
       |
       |  val baseAmount         = BaseAmount
       |  val feeNum             = FeeNum
       |  val feeDenom           = FeeDenom
       |  val maxExFee           = MaxExFee
       |  val exFeePerTokenDenom = ExFeePerTokenDenom
       |  val exFeePerTokenNum   = ExFeePerTokenNum
       |  val minQuoteAmount     = MinQuoteAmount
       |
       |  val poolIn = INPUTS(0)
       |
       |  // Validations
       |  // 1.
       |  val validTrade =
       |    if (INPUTS.size >= 2 && poolIn.tokens.size == 4) {
       |
       |      val poolNFT    = poolIn.tokens(0)._1
       |      val poolAssetX = poolIn.tokens(2)
       |      val poolAssetY = poolIn.tokens(3)
       |
       |      val validPoolIn = poolNFT == PoolNFT
       |
       |      val rewardBox  = OUTPUTS(1)
       |      val quoteAsset = rewardBox.tokens(0)
       |      val quoteAmount =
       |        if (SpectrumIsQuote) {
       |          val deltaQuote = quoteAsset._2.toBigInt - maxExFee
       |          deltaQuote.toBigInt * exFeePerTokenDenom / (exFeePerTokenDenom - exFeePerTokenNum)
       |        } else {
       |          quoteAsset._2.toBigInt
       |        }
       |      // 1.1.
       |      val valuePreserved = rewardBox.value >= SELF.value
       |      // 1.2.
       |      val fairExFee =
       |        if (SpectrumIsQuote) true
       |        else {
       |          val exFee     = quoteAmount * exFeePerTokenNum / exFeePerTokenDenom
       |          val remainder = maxExFee - exFee
       |          if (remainder > 0) {
       |            val spectrumRem = rewardBox.tokens(1)
       |            spectrumRem._1 == SpectrumId && spectrumRem._2 >= remainder
       |          } else {
       |            true
       |          }
       |        }
       |
       |      val relaxedOutput = quoteAmount + 1L // handle rounding loss
       |      val poolX         = poolAssetX._2.toBigInt
       |      val poolY         = poolAssetY._2.toBigInt
       |      val base_x_feeNum = baseAmount.toBigInt * feeNum
       |      // 1.3.
       |      val fairPrice =
       |        if (poolAssetX._1 == QuoteId) {
       |          poolX * base_x_feeNum <= relaxedOutput * (poolY * feeDenom + base_x_feeNum)
       |        } else {
       |          poolY * base_x_feeNum <= relaxedOutput * (poolX * feeDenom + base_x_feeNum)
       |        }
       |      // 1.4.
       |      val validMinerFee = OUTPUTS.map { (o: Box) =>
       |        if (o.propositionBytes == MinerPropBytes) o.value else 0L
       |      }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee
       |
       |      validPoolIn &&
       |      rewardBox.propositionBytes == RedeemerPropBytes &&
       |      quoteAsset._1 == QuoteId &&
       |      quoteAsset._2 >= minQuoteAmount &&
       |      valuePreserved &&
       |      fairExFee &&
       |      fairPrice &&
       |      validMinerFee
       |
       |    } else false
       |
       |  sigmaProp(RefundProp || validTrade)
       |}""".stripMargin
}
