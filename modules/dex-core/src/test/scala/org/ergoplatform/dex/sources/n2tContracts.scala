package org.ergoplatform.dex.sources

object n2tContracts {

  val depositV3 =
    s"""
       |{ // ===== Contract Information ===== //
       |  // Name: Deposit
       |  // Description: Contract that validates user's deposit into the CFMM n2t Pool.
       |  //
       |  // Constants:
       |  //
       |  // {1}  -> SelfXAmount[Long]
       |  // {2}  -> RefundProp[ProveDlog]
       |  // {9}  -> SelfYAmount[Long] SELF.tokens(1)._2 - ExFee if Y is SPF else SELF.tokens(1)._2
       |  // {12} -> PoolNFT[Coll[Byte]]
       |  // {13} -> RedeemerPropBytes[Coll[Byte]]
       |  // {18} -> MinerPropBytes[Coll[Byte]]
       |  // {21} -> MaxMinerFee[Long]
       |  //
       |  // ErgoTree: 19bf0417040005c0b80208cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d040404060402040205feffffffffffffffff01040405e0d403040004000e2002020202020202020202020202020202020202020202020202020202020202020e2001010101010101010101010101010101010101010101010101010101010101010404040205c0b80201000e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100d802d601b2a4730000d6027301eb027302d195ed92b1a4730393b1db630872017304d80bd603db63087201d604b2a5730500d605b27203730600d6067e9973078c72050206d6077ec1720106d6089d9c7e72020672067207d609b27203730800d60a7e8c72090206d60b9d9c7e7309067206720ad60cdb63087204d60db2720c730a00ededededed938cb27203730b0001730c93c27204730d95ed8f7208720b93b1720c730ed801d60eb2720c730f00eded92c1720499c1a77310938c720e018c720901927e8c720e02069d9c99720b7208720a720695927208720b927ec1720406997ec1a706997e7202069d9c997208720b720772067311938c720d018c720501927e8c720d0206a17208720b90b0ada5d9010e639593c2720e7312c1720e73137314d9010e599a8c720e018c720e0273157316
       |  //
       |  // ErgoTreeTemplate: d802d601b2a4730000d6027301eb027302d195ed92b1a4730393b1db630872017304d80bd603db63087201d604b2a5730500d605b27203730600d6067e9973078c72050206d6077ec1720106d6089d9c7e72020672067207d609b27203730800d60a7e8c72090206d60b9d9c7e7309067206720ad60cdb63087204d60db2720c730a00ededededed938cb27203730b0001730c93c27204730d95ed8f7208720b93b1720c730ed801d60eb2720c730f00eded92c1720499c1a77310938c720e018c720901927e8c720e02069d9c99720b7208720a720695927208720b927ec1720406997ec1a706997e7202069d9c997208720b720772067311938c720d018c720501927e8c720d0206a17208720b90b0ada5d9010e639593c2720e7312c1720e73137314d9010e599a8c720e018c720e0273157316
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
       |    if (INPUTS.size >= 2 && poolIn.tokens.size == 3) {
       |
       |      // 1.1.
       |      val validPoolIn = poolIn.tokens(0)._1 == PoolNFT
       |
       |      val poolLP          = poolIn.tokens(1)
       |      val reservesXAmount = poolIn.value
       |      val reservesY       = poolIn.tokens(2)
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
       |      val validChange =
       |        if (minByX < minByY && rewardOut.tokens.size == 2) {
       |          val diff    = minByY - minByX
       |          val excessY = diff * reservesYAmount / supplyLP
       |          val changeY = rewardOut.tokens(1)
       |
       |          rewardOut.value >= SELF.value - selfXAmount &&
       |          changeY._1 == reservesY._1 &&
       |          changeY._2 >= excessY
       |
       |        } else if (minByX >= minByY) {
       |          val diff    = minByX - minByY
       |          val excessX = diff * reservesXAmount / supplyLP
       |
       |          rewardOut.value >= SELF.value - (selfXAmount - excessX)
       |
       |        } else {
       |          false
       |        }
       |      // 1.3.
       |      val validMinerFee = OUTPUTS.map { (o: Box) =>
       |        if (o.propositionBytes == MinerPropBytes) o.value else 0L
       |      }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee
       |
       |      validPoolIn &&
       |      rewardOut.propositionBytes == RedeemerPropBytes &&
       |      validChange &&
       |      rewardLP._1 == poolLP._1 &&
       |      rewardLP._2 >= minimalReward &&
       |      validMinerFee
       |
       |    } else false
       |
       |  sigmaProp(RefundProp || validDeposit)
       |}
       |""".stripMargin

  val redeemV3 =
    s"""
       |{ // ===== Contract Information ===== //
       |  // Name: Redeem
       |  // Description: Contract that validates user's redeem from the CFMM n2t Pool.
       |  //
       |  // Constants:
       |  //
       |  // {1}  -> RefundProp[ProveDlog]
       |  // {11} -> PoolNFT[Coll[Byte]]
       |  // {12} -> RedeemerPropBytes[Coll[Byte]]
       |  // {13} -> MinerPropBytes[Coll[Byte]]
       |  // {16} -> MaxMinerFee[Long]
       |  //
       |  // ErgoTree: 19c50312040008cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d04040406040204000404040005feffffffffffffffff01040204000e2002020202020202020202020202020202020202020202020202020202020202020e2001010101010101010101010101010101010101010101010101010101010101010e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100d801d601b2a4730000eb027301d195ed92b1a4730293b1db630872017303d806d602db63087201d603b2a5730400d604b2db63087203730500d605b27202730600d6067e8cb2db6308a77307000206d6077e9973088cb272027309000206ededededed938cb27202730a0001730b93c27203730c938c7204018c720501927e99c17203c1a7069d9c72067ec17201067207927e8c720402069d9c72067e8c72050206720790b0ada5d90108639593c27208730dc17208730e730fd90108599a8c7208018c72080273107311
       |  //
       |  // ErgoTreeTemplate: d801d601b2a4730000eb027301d195ed92b1a4730293b1db630872017303d806d602db63087201d603b2a5730400d604b2db63087203730500d605b27202730600d6067e8cb2db6308a77307000206d6077e9973088cb272027309000206ededededed938cb27202730a0001730b93c27203730c938c7204018c720501927e99c17203c1a7069d9c72067ec17201067207927e8c720402069d9c72067e8c72050206720790b0ada5d90108639593c27208730dc17208730e730fd90108599a8c7208018c72080273107311
       |
       |  val InitiallyLockedLP = 0x7fffffffffffffffL
       |
       |  val poolIn = INPUTS(0)
       |
       |  // Validations
       |  // 1.
       |  val validRedeem =
       |    if (INPUTS.size >= 2 && poolIn.tokens.size == 3) {
       |      val selfLP = SELF.tokens(0)
       |      // 1.1.
       |      val validPoolIn = poolIn.tokens(0)._1 == PoolNFT
       |
       |      val poolLP          = poolIn.tokens(1)
       |      val reservesXAmount = poolIn.value
       |      val reservesY       = poolIn.tokens(2)
       |
       |      val supplyLP = InitiallyLockedLP - poolLP._2
       |
       |      val minReturnX = selfLP._2.toBigInt * reservesXAmount / supplyLP
       |      val minReturnY = selfLP._2.toBigInt * reservesY._2 / supplyLP
       |
       |      val returnOut = OUTPUTS(1)
       |
       |      val returnXAmount = returnOut.value - SELF.value
       |      val returnY       = returnOut.tokens(0)
       |      // 1.2.
       |      val validMinerFee = OUTPUTS.map { (o: Box) =>
       |        if (o.propositionBytes == MinerPropBytes) o.value else 0L}
       |        .fold(0L, { ( a: Long, b: Long) => a + b } ) <= MaxMinerFee
       |
       |      validPoolIn &&
       |      returnOut.propositionBytes == RedeemerPropBytes &&
       |      returnY._1 == reservesY._1 && // token id matches
       |      returnXAmount >= minReturnX &&
       |      returnY._2 >= minReturnY &&
       |      validMinerFee
       |
       |    } else false
       |
       |  sigmaProp(RefundProp || validRedeem)
       |}
       |""".stripMargin

  val swapBuyV3: String =
    s"""
       |{ // ===== Contract Information ===== //
       |  // Name: SwapBuy
       |  // Description: Contract that validates user's swap from token to ERG in the CFMM n2t Pool.
       |  //
       |  // Constants:
       |  //
       |  // {1}  -> BaseAmount[Long]
       |  // {2}  -> FeeNum[Int]
       |  // {3}  -> RefundProp[ProveDlog]
       |  // {7}  -> MaxExFee[Long]
       |  // {8}  -> ExFeePerTokenDenom[Long]
       |  // {9}  -> ExFeePerTokenNum[Long]
       |  // {11} -> PoolNFT[Coll[Byte]]
       |  // {12} -> RedeemerPropBytes[Coll[Byte]]
       |  // {13} -> MinQuoteAmount[Long]
       |  // {16} -> SpectrumId[Coll[Byte]]
       |  // {20} -> FeeDenom[Int]
       |  // {21} -> MinerPropBytes[Coll[Byte]]
       |  // {24} -> MaxMinerFee[Long]
       |  //
       |  // ErgoTree: 198b041a040005e01204c80f08cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d04040406040205f015052c05c80104000e2002020202020202020202020202020202020202020202020202020202020202020e20010101010101010101010101010101010101010101010101010101010101010105c00c06010004000e20030303030303030303030303030303030303030303030303030303030303030301010502040404d00f0e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100d802d601b2a4730000d6029c73017e730205eb027303d195ed92b1a4730493b1db630872017305d804d603db63087201d604b2a5730600d60599c17204c1a7d606997e7307069d9c7e7205067e7308067e730906ededededed938cb27203730a0001730b93c27204730c927205730d95917206730ed801d607b2db63087204730f00ed938c7207017310927e8c7207020672067311909c7ec17201067e7202069c7e9a72057312069a9c7e8cb2720373130002067e7314067e72020690b0ada5d90107639593c272077315c1720773167317d90107599a8c7207018c72070273187319
       |  //
       |  // ErgoTreeTemplate: d802d601b2a4730000d6029c73017e730205eb027303d195ed92b1a4730493b1db630872017305d804d603db63087201d604b2a5730600d60599c17204c1a7d606997e7307069d9c7e7205067e7308067e730906ededededed938cb27203730a0001730b93c27204730c927205730d95917206730ed801d607b2db63087204730f00ed938c7207017310927e8c7207020672067311909c7ec17201067e7202069c7e9a72057312069a9c7e8cb2720373130002067e7314067e72020690b0ada5d90107639593c272077315c1720773167317d90107599a8c7207018c72070273187319
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
       |    if (INPUTS.size >= 2 && poolIn.tokens.size == 3) {
       |      val poolNFT = poolIn.tokens(0)._1 // first token id is NFT
       |
       |      val poolReservesX = poolIn.value.toBigInt // nanoErgs is X asset amount
       |      val poolReservesY = poolIn.tokens(2)._2.toBigInt // third token amount is Y asset amount
       |
       |      val validPoolIn = poolNFT == PoolNFT
       |
       |      val rewardBox = OUTPUTS(1)
       |
       |      val quoteAmount = rewardBox.value - SELF.value
       |      // 1.1.
       |      val fairExFee = {
       |        val exFee     = quoteAmount.toBigInt * exFeePerTokenNum / exFeePerTokenDenom
       |        val remainder = maxExFee - exFee
       |        if (remainder > 0) {
       |          val spectrumRem = rewardBox.tokens(0)
       |          spectrumRem._1 == SpectrumId && spectrumRem._2 >= remainder
       |        } else {
       |          true
       |        }
       |      }
       |      val relaxedOutput = quoteAmount + 1L // handle rounding loss
       |      val base_x_feeNum = baseAmount.toBigInt * feeNum
       |      // 1.2.
       |      val fairPrice = poolReservesX * base_x_feeNum <= relaxedOutput * (poolReservesY * feeDenom + base_x_feeNum)
       |      // 1.3.
       |      val validMinerFee = OUTPUTS.map { (o: Box) =>
       |        if (o.propositionBytes == MinerPropBytes) o.value else 0L
       |      }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee
       |
       |      validPoolIn &&
       |      rewardBox.propositionBytes == RedeemerPropBytes &&
       |      quoteAmount >= minQuoteAmount &&
       |      fairExFee &&
       |      fairPrice &&
       |      validMinerFee
       |
       |    } else false
       |
       |  sigmaProp(RefundProp || validTrade)
       |}
       |""".stripMargin

  val swapSellV3: String =
    """
      |{ // ===== Contract Information ===== //
      |  // Name: SwapSell
      |  // Description: Contract that validates user's swap from ERG to token in the CFMM n2t Pool.
      |  //
      |  // Constants:
      |  //
      |  // {1} -> ExFeePerTokenDenom[Long]
      |  // {2} -> Delta[Long]
      |  // {3} -> BaseAmount[Long]
      |  // {4} -> FeeNum[Int]
      |  // {5} -> RefundProp[ProveDlog]
      |  // {10} -> SpectrumIsQuote[Boolean]
      |  // {11} -> MaxExFee[Long]
      |  // {13} -> PoolNFT[Coll[Byte]]
      |  // {14} -> RedeemerPropBytes[Coll[Byte]]
      |  // {15} -> QuoteId[Coll[Byte]]
      |  // {16} -> MinQuoteAmount[Long]
      |  // {23} -> SpectrumId[Coll[Byte]]
      |  // {27} -> FeeDenom[Int]
      |  // {28} -> MinerPropBytes[Coll[Byte]]
      |  // {31} -> MaxMinerFee[Long]
      |  //
      |  // ErgoTree: 19fe04210400059cdb0205cead0105e01204c80f08cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d0404040604020400010105f01504000e2002020202020202020202020202020202020202020202020202020202020202020e2001010101010101010101010101010101010101010101010101010101010101010e20040404040404040404040404040404040404040404040404040404040404040405c00c0101010105f015060100040404020e2003030303030303030303030303030303030303030303030303030303030303030101040406010104d00f0e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100d804d601b2a4730000d6027301d6037302d6049c73037e730405eb027305d195ed92b1a4730693b1db630872017307d806d605db63087201d606b2a5730800d607db63087206d608b27207730900d6098c720802d60a95730a9d9c7e997209730b067e7202067e7203067e720906edededededed938cb27205730c0001730d93c27206730e938c720801730f92720a7e7310069573117312d801d60b997e7313069d9c720a7e7203067e72020695ed91720b731492b172077315d801d60cb27207731600ed938c720c017317927e8c720c0206720b7318909c7e8cb2720573190002067e7204069c9a720a731a9a9c7ec17201067e731b067e72040690b0ada5d9010b639593c2720b731cc1720b731d731ed9010b599a8c720b018c720b02731f7320
      |  //
      |  // ErgoTreeTemplate: d804d601b2a4730000d6027301d6037302d6049c73037e730405eb027305d195ed92b1a4730693b1db630872017307d806d605db63087201d606b2a5730800d607db63087206d608b27207730900d6098c720802d60a95730a9d9c7e997209730b067e7202067e7203067e720906edededededed938cb27205730c0001730d93c27206730e938c720801730f92720a7e7310069573117312d801d60b997e7313069d9c720a7e7203067e72020695ed91720b731492b172077315d801d60cb27207731600ed938c720c017317927e8c720c0206720b7318909c7e8cb2720573190002067e7204069c9a720a731a9a9c7ec17201067e731b067e72040690b0ada5d9010b639593c2720b731cc1720b731d731ed9010b599a8c720b018c720b02731f7320
      |
      |  val baseAmount         = BaseAmount
      |  val feeNum             = FeeNum
      |  val feeDenom           = FeeDenom
      |  val maxExFee           = MaxExFee
      |  val exFeePerTokenDenom = ExFeePerTokenDenom
      |  val delta              = Delta
      |  val minQuoteAmount     = MinQuoteAmount
      |
      |  val poolIn = INPUTS(0)
      |
      |  // Validations
      |  // 1.
      |  val validTrade =
      |    if (INPUTS.size >= 2 && poolIn.tokens.size == 3) {
      |      val poolNFT = poolIn.tokens(0)._1
      |
      |      val poolY = poolIn.tokens(2)
      |
      |      val poolReservesX = poolIn.value.toBigInt
      |      val poolReservesY = poolY._2.toBigInt
      |      val validPoolIn   = poolNFT == PoolNFT
      |
      |      val rewardBox = OUTPUTS(1)
      |
      |      val quoteAsset = rewardBox.tokens(0)
      |      val quoteAmount =
      |        if (SpectrumIsQuote) {
      |          val quoteAssetAmount = quoteAsset._2
      |          val deltaQuote       = quoteAssetAmount - maxExFee
      |          (deltaQuote.toBigInt * exFeePerTokenDenom) / delta
      |        } else {
      |          quoteAsset._2.toBigInt
      |        }
      |      // 1.1.
      |      val fairExFee =
      |        if (SpectrumIsQuote) true
      |        else {
      |          val exFeePerTokenNum = exFeePerTokenDenom - delta
      |          val exFee            = quoteAmount * exFeePerTokenNum / exFeePerTokenDenom
      |          val remainder        = maxExFee - exFee
      |          if (remainder > 0 && rewardBox.tokens.size >= 2) {
      |            val spectrumRem = rewardBox.tokens(1)
      |            spectrumRem._1 == SpectrumId && spectrumRem._2 >= remainder
      |          } else {
      |            true
      |          }
      |        }
      |
      |      val relaxedOutput = quoteAmount + 1L // handle rounding loss
      |
      |      val base_x_feeNum = baseAmount.toBigInt * feeNum
      |      // 1.2.
      |      val fairPrice = poolReservesY * base_x_feeNum <= relaxedOutput * (poolReservesX * feeDenom + base_x_feeNum)
      |      // 1.3.
      |      val validMinerFee = OUTPUTS.map { (o: Box) =>
      |        if (o.propositionBytes == MinerPropBytes) o.value else 0L
      |      }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee
      |
      |      validPoolIn &&
      |      rewardBox.propositionBytes == RedeemerPropBytes &&
      |      quoteAsset._1 == QuoteId &&
      |      quoteAmount >= minQuoteAmount &&
      |      fairExFee &&
      |      fairPrice &&
      |      validMinerFee
      |
      |    } else false
      |
      |  sigmaProp(RefundProp || validTrade)
      |}""".stripMargin

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
      |            val reservesYAmount = reservesY._2
      |
      |            val supplyLP = InitiallyLockedLP - poolLP._2
      |
      |            val _selfX = SelfX
      |
      |            val minByX = _selfX.toBigInt * supplyLP / reservesXAmount
      |            val minByY = selfY._2.toBigInt * supplyLP / reservesYAmount
      |
      |            val minimalReward = min(minByX, minByY)
      |
      |            val rewardOut = OUTPUTS(1)
      |            val rewardLP  = rewardOut.tokens(0)
      |
      |            val validChange =
      |              if (minByX < minByY && rewardOut.tokens.size == 2) {
      |                val diff = minByY - minByX
      |                val excessY = diff * reservesYAmount / supplyLP
      |
      |                val changeY = rewardOut.tokens(1)
      |
      |                rewardOut.value >= SELF.value - DexFee - _selfX &&
      |                changeY._1 == reservesY._1 &&
      |                changeY._2 >= excessY
      |              } else if (minByX >= minByY) {
      |                val diff = minByX - minByY
      |                val excessX = diff * reservesXAmount / supplyLP
      |
      |                rewardOut.value >= SELF.value - DexFee - (_selfX - excessX)
      |              } else {
      |                false
      |              }
      |
      |            val validMinerFee = OUTPUTS.map { (o: Box) =>
      |                if (o.propositionBytes == MinerPropBytes) o.value else 0L
      |            }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee
      |
      |            validPoolIn &&
      |            rewardOut.propositionBytes == Pk.propBytes &&
      |            validChange &&
      |            rewardLP._1 == poolLP._1 &&
      |            rewardLP._2 >= minimalReward &&
      |            validMinerFee
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
      |            val validMinerFee = OUTPUTS.map { (o: Box) =>
      |                if (o.propositionBytes == MinerPropBytes) o.value else 0L
      |            }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee
      |
      |            validPoolIn &&
      |            returnOut.propositionBytes == Pk.propBytes &&
      |            returnY._1 == reservesY._1 && // token id matches
      |            returnXAmount >= minReturnX &&
      |            returnY._2 >= minReturnY &&
      |            validMinerFee
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
      |            val validMinerFee = OUTPUTS.map { (o: Box) =>
      |                if (o.propositionBytes == MinerPropBytes) o.value else 0L
      |            }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee
      |
      |            validPoolIn &&
      |            rewardBox.propositionBytes == Pk.propBytes &&
      |            quoteAsset._1 == QuoteId &&
      |            quoteAmount >= MinQuoteAmount &&
      |            fairDexFee &&
      |            fairPrice &&
      |            validMinerFee
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
      |            val validMinerFee = OUTPUTS.map { (o: Box) =>
      |                if (o.propositionBytes == MinerPropBytes) o.value else 0L
      |            }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee
      |
      |            validPoolIn &&
      |            rewardBox.propositionBytes == Pk.propBytes &&
      |            quoteAmount >= MinQuoteAmount &&
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

  val swapBuyV2: String =
     """
       |{   // Token -> ERG
       |    val FeeDenom            = 1000
       |    val FeeNum              = 996
       |    val DexFeePerTokenNum   = 10000000L
       |    val DexFeePerTokenDenom = 5L
       |    val MinQuoteAmount      = 3L
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
       |            val validMinerFee = OUTPUTS.map { (o: Box) =>
       |                if (o.propositionBytes == MinerPropBytes) o.value else 0L
       |            }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee
       |
       |            validPoolIn &&
       |            rewardBox.propositionBytes == RedeemerPropBytes &&
       |            quoteAmount >= MinQuoteAmount &&
       |            fairPrice &&
       |            validMinerFee
       |        } else false
       |
       |    sigmaProp(RefundProp || validTrade)
       |}
       |""".stripMargin

  val swapSellV2: String =
    """
      |{   // ERG -> Token
      |    val FeeDenom            = 1000
      |    val FeeNum              = 996
      |    val DexFeePerTokenNum   = 101L
      |    val DexFeePerTokenDenom = 105L
      |    val MinQuoteAmount      = 3L
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
      |            val validMinerFee = OUTPUTS.map { (o: Box) =>
      |                if (o.propositionBytes == MinerPropBytes) o.value else 0L
      |            }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee
      |
      |            validPoolIn &&
      |            rewardBox.propositionBytes == RedeemerPropBytes &&
      |            quoteAsset._1 == QuoteId &&
      |            quoteAmount >= MinQuoteAmount &&
      |            fairDexFee &&
      |            fairPrice &&
      |            validMinerFee
      |        } else false
      |
      |    sigmaProp(RefundProp || validTrade)
      |}
      |""".stripMargin
}
