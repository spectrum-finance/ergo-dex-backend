package org.ergoplatform.dex.demo

import io.circe.syntax._
import org.bouncycastle.util.BigIntegers
import org.ergoplatform.ErgoBox.TokenId
import org.ergoplatform._
import org.ergoplatform.dex.protocol.codecs._
import org.ergoplatform.wallet.interpreter.ErgoUnsafeProver
import scorex.crypto.authds.ADKey
import scorex.crypto.hash.{Blake2b256, Digest32}
import scorex.util.encode.Base16
import sigmastate.Values.ErgoTree
import sigmastate.basics.DLogProtocol.DLogProverInput
import sigmastate.eval._
import sigmastate.lang.SigmaCompiler
import sigmastate.lang.Terms.ValueOps

object MakeSwapProxy extends App {

  val secret     = ""
  val inputId    = "0f15c8e5254f3a623f77cb547f36d12cd6e7041a1a07cd2d8425e6742f83467d"
  val poolInId   = "9057994837cf063588c0b85257c8f1a617f2a1a5e0f69ea27cd1ea0d2d9fda90"
  val inputValue = 1859599950L
  val recvAddr   = "9hWMWtGho2VBPsSRigWMUUtk9sWWPFKSdDWcxSvV9TiTB4PCRKc"
  val curHeight  = 415703

  implicit val IR: IRContext         = new CompiletimeIRContext()
  implicit val e: ErgoAddressEncoder = ErgoAddressEncoder(ErgoAddressEncoder.MainnetNetworkPrefix)
  val sigma                          = SigmaCompiler(ErgoAddressEncoder.MainnetNetworkPrefix)

  val sk          = DLogProverInput(BigIntegers.fromUnsignedByteArray(Base16.decode(secret).get))
  val pk          = sk.publicImage
  val selfAddress = P2PKAddress(pk)

  val SafeMinAmountNErg = 400000L

  val tergId  = Digest32 @@ Base16.decode("f45c4f0d95ce1c64defa607d94717a9a30c00fdd44827504be20db19f4dce36f").get
  val tusdId  = Digest32 @@ Base16.decode("f302c6c042ada3566df8e67069f8ac76d10ce15889535141593c168f3c59e776").get
  val edextId = Digest32 @@ Base16.decode("7c232b68665d233356e9abadf3820abff725105c5ccfa8618b77bc3a8bf603ce").get

  // User input
  val input = new UnsignedInput(ADKey @@ Base16.decode(inputId).get)

  val balanceTErg  = 997000000000L
  val balanceTUsd  = 99999699993969709L
  val balanceEdext = 88L

  // Pool input
  val poolIn = new UnsignedInput(ADKey @@ Base16.decode(poolInId).get)

  val poolProp = ErgoTree.fromProposition(sigma.compile(Map.empty, contracts.poolContract).asSigmaProp)
  val poolSH   = Blake2b256.hash(poolProp.bytes)

  val reservesTErg0 = 1000000000L
  val reservesTUsd0 = 100000000000L

  def inputAmount(tokenId: TokenId, output: Long) =
    if (tokenId == tergId) (reservesTUsd0 * output * 1000 / ((reservesTErg0 - output) * 995)) + 1
    else (reservesTErg0 * output * 1000 / ((reservesTUsd0 - output) * 995)) + 1

  // Order params
  val tergOut = 20000L
  val tusdIn  = inputAmount(tergId, tergOut)

  val dexFeePerTokenNErg = 50L
  val dexFeeNErg         = tergOut * dexFeePerTokenNErg

  require(dexFeeNErg >= SafeMinAmountNErg)

  val minerFeeNErg = 1000000L

  require(BigInt(reservesTErg0) * tusdIn * 995 >= tergOut * (BigInt(reservesTUsd0) * 1000 + tusdIn * 995))

  val swapEnv = Map(
    "pk"             -> pk,
    "poolScriptHash" -> poolSH.!@@(Digest32),
    "dexFeePerToken" -> dexFeePerTokenNErg,
    "minerFee"       -> minerFeeNErg,
    "minQuoteAmount" -> tergOut,
    "quoteId"        -> tergId.!@@(Digest32),
    "poolFeeNum"     -> 995L
  )
  val swapProp = ErgoTree.fromProposition(sigma.compile(swapEnv, contracts.swapContract).asSigmaProp)

  val swap = new ErgoBoxCandidate(
    SafeMinAmountNErg + dexFeeNErg + minerFeeNErg,
    swapProp,
    curHeight,
    additionalTokens = Colls.fromItems(tusdId -> tusdIn)
  )

  val feeAddress = Pay2SAddress(ErgoScriptPredef.feeProposition())
  val minerFee   = new ErgoBoxCandidate(minerFeeNErg, feeAddress.script, curHeight)

  val change = new ErgoBoxCandidate(
    inputValue - swap.value - minerFee.value,
    selfAddress.script,
    curHeight,
    additionalTokens = Colls.fromItems(tergId -> balanceTErg, tusdId -> (balanceTUsd - tusdIn), edextId -> balanceEdext)
  )

  val inputs = Vector(input)
  val outs   = Vector(swap, change, minerFee)
  val utx    = UnsignedErgoLikeTransaction(inputs, outs)
  val tx     = ErgoUnsafeProver.prove(utx, sk)

  println(tx.asJson.noSpacesSortKeys)
}
