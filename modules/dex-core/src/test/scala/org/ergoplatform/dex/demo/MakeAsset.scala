package org.ergoplatform.dex.demo

import io.circe.syntax._
import org.bouncycastle.util.BigIntegers
import org.ergoplatform.ErgoBox.{NonMandatoryRegisterId, R4, TokenId}
import org.ergoplatform._
import org.ergoplatform.dex.protocol.codecs._
import scorex.crypto.authds.ADKey
import scorex.crypto.hash.{Blake2b256, Digest32}
import scorex.util.encode.Base16
import sigmastate.Values.{ErgoTree, LongConstant}
import sigmastate.basics.DLogProtocol.DLogProverInput
import sigmastate.eval._
import sigmastate.interpreter.ProverResult
import sigmastate.lang.SigmaCompiler
import sigmastate.lang.Terms.ValueOps

object MakeAsset extends App {

  val secret     = ""
  val swapId     = "3b6e6939f107eebd84e9adacc081732ef1ff4cca6e4b26996685ae278503efc9"
  val poolInId   = "9057994837cf063588c0b85257c8f1a617f2a1a5e0f69ea27cd1ea0d2d9fda90"
  val inputValue = 2400000L
  val recvAddr   = "9hWMWtGho2VBPsSRigWMUUtk9sWWPFKSdDWcxSvV9TiTB4PCRKc"
  val curHeight  = 415703

  implicit val IR: IRContext         = new CompiletimeIRContext()
  implicit val e: ErgoAddressEncoder = ErgoAddressEncoder(ErgoAddressEncoder.MainnetNetworkPrefix)
  val sigma                          = SigmaCompiler(ErgoAddressEncoder.MainnetNetworkPrefix)

  val sk          = DLogProverInput(BigIntegers.fromUnsignedByteArray(Base16.decode(secret).get))
  val pk          = sk.publicImage
  val selfAddress = P2PKAddress(pk)

  val SafeMinAmountNErg = 360000L

  val nftId   = Digest32 @@ (Base16.decode("d7d0d6ecc4ed6c282a22655da253070157b381173460a6a8f556eace1529791e").get)
  val lpId    = Digest32 @@ (Base16.decode("dbd9424f420cc104eb4cdddb3ffb1e67f60900c1c23eed73b02dce87e70ff327").get)
  val tergId  = Digest32 @@ (Base16.decode("f45c4f0d95ce1c64defa607d94717a9a30c00fdd44827504be20db19f4dce36f").get)
  val tusdId  = Digest32 @@ (Base16.decode("f302c6c042ada3566df8e67069f8ac76d10ce15889535141593c168f3c59e776").get)
  val edextId = Digest32 @@ (Base16.decode("7c232b68665d233356e9abadf3820abff725105c5ccfa8618b77bc3a8bf603ce").get)

  // User input
  val swapIn = new Input(ADKey @@ Base16.decode(swapId).get, ProverResult.empty)

  val balanceTErg  = 997000000000L
  val balanceTUsd  = 99999700000000000L
  val balanceEdext = 88L

  // Pool input
  val poolIn = new Input(ADKey @@ Base16.decode(poolInId).get, ProverResult.empty)

  val poolProp = ErgoTree.fromProposition(sigma.compile(Map.empty, poolContract).asSigmaProp)
  val poolSH   = Blake2b256.hash(poolProp.bytes)

  val reservesTErg0 = 1000000000L
  val reservesTUsd0 = 100000000000L

  val poolLP0 = 999999997213195445L

  def inputAmount(tokenId: TokenId, output: Long) =
    if (tokenId == tergId) (reservesTUsd0 * output * 1000 / ((reservesTErg0 - output) * 995)) + 1
    else (reservesTErg0 * output * 1000 / ((reservesTUsd0 - output) * 995)) + 1

  // Order params
  val tergOut = 20000L
  val tusdIn  = inputAmount(tergId, tergOut)

  val dexFeePerTokenNErg = 50L
  val dexFeeNErg         = tergOut * dexFeePerTokenNErg

  val minerFeeNErg = 1000000L

  val pool1 = new ErgoBoxCandidate(
    2000000L,
    poolProp,
    curHeight,
    additionalTokens = Colls.fromItems(
      nftId  -> 1L,
      lpId   -> poolLP0,
      tergId -> (reservesTErg0 - tergOut),
      tusdId -> (reservesTUsd0 + tusdIn)
    ),
    additionalRegisters = scala.Predef.Map(
      (R4: NonMandatoryRegisterId) -> LongConstant(995L)
    )
  )

  val feeAddress = Pay2SAddress(ErgoScriptPredef.feeProposition())
  val minerFee   = new ErgoBoxCandidate(minerFeeNErg, feeAddress.script, curHeight)

  val reward = new ErgoBoxCandidate(
    inputValue - minerFee.value,
    selfAddress.script,
    curHeight,
    additionalTokens = Colls.fromItems(tergId -> tergOut)
  )

  val inputs = Vector(poolIn, swapIn)
  val outs   = Vector(pool1, reward, minerFee)
  val tx     = ErgoLikeTransaction(inputs, outs)

  println(tx.asJson.noSpacesSortKeys)

  def poolContract =
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
