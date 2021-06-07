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

object ExecuteSwap extends App {

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

  val poolProp = ErgoTree.fromProposition(sigma.compile(Map.empty, contracts.pool).asSigmaProp)
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
}
