package org.ergoplatform.dex.demo

import io.circe.syntax._
import org.bouncycastle.util.BigIntegers
import org.ergoplatform.ErgoBox._
import org.ergoplatform._
import org.ergoplatform.dex.protocol.codecs._
import org.ergoplatform.wallet.interpreter.ErgoUnsafeProver
import scorex.crypto.authds.ADKey
import scorex.crypto.hash.{Blake2b256, Digest32}
import scorex.util.encode.Base16
import sigmastate.Values.{ByteArrayConstant, ErgoTree, LongConstant}
import sigmastate.basics.DLogProtocol.DLogProverInput
import sigmastate.eval.{IRContext, _}
import sigmastate.lang.SigmaCompiler
import sigmastate.lang.Terms.ValueOps

object BootAmm extends App {

  val secret     = ""
  val inputId    = "dbd9424f420cc104eb4cdddb3ffb1e67f60900c1c23eed73b02dce87e70ff327"
  val inputValue = 1873000000L
  val curHeight  = 473151

  implicit val IR: IRContext         = new CompiletimeIRContext()
  implicit val e: ErgoAddressEncoder = ErgoAddressEncoder(ErgoAddressEncoder.MainnetNetworkPrefix)
  val sigma                          = SigmaCompiler(ErgoAddressEncoder.MainnetNetworkPrefix)

  val sk          = DLogProverInput(BigIntegers.fromUnsignedByteArray(Base16.decode(secret).get))
  val pk          = sk.publicImage
  val selfAddress = P2PKAddress(pk)

  val input = new UnsignedInput(ADKey @@ Base16.decode(inputId).get)

  val edextId     = Digest32 @@ (Base16.decode("7c232b68665d233356e9abadf3820abff725105c5ccfa8618b77bc3a8bf603ce").get)
  val edextAmount = 88L
  val tergId      = Digest32 @@ (Base16.decode("f45c4f0d95ce1c64defa607d94717a9a30c00fdd44827504be20db19f4dce36f").get)
  val tergAmount  = 998000000000L
  val tusdId      = Digest32 @@ (Base16.decode("f302c6c042ada3566df8e67069f8ac76d10ce15889535141593c168f3c59e776").get)
  val tusdAmount  = 99999800000000000L

  val lpId     = Digest32 @@ (ADKey !@@ input.boxId)
  val lpAmount = 1000000000000000000L
  val lpName   = "TERG_TUSD_LP"

  val bootScript = ErgoTree.fromProposition(sigma.compile(Map.empty, contracts.bootContract).asSigmaProp)
  val poolScript = ErgoTree.fromProposition(sigma.compile(Map.empty, contracts.poolContract).asSigmaProp)
  val poolSH     = Blake2b256.hash(poolScript.bytes)

  val bootNErg = 4000000L

  val bootTErg = 1000000000L
  val bootTUsd = 100000000000L

  val feeNErgs = 1000000L

  require(bootTErg * bootTUsd <= Long.MaxValue)
  val sharesLP = math.sqrt(bootTErg * bootTUsd).toLong
  println(sharesLP)
  println(math.sqrt(sharesLP * sharesLP).toLong)
  require(sharesLP * sharesLP <= bootTErg * bootTUsd)

  val poolFee = 995L

  val bootRegisters = scala.Predef.Map(
    R4 -> ByteArrayConstant(lpName.getBytes("UTF-8")),
    R5 -> ByteArrayConstant(poolSH),
    R6 -> LongConstant(sharesLP),
    R7 -> LongConstant(poolFee),
    R8 -> LongConstant(feeNErgs),
    R9 -> ByteArrayConstant(selfAddress.script.bytes)
  )

  val boot = new ErgoBoxCandidate(
    bootNErg,
    bootScript,
    curHeight,
    additionalTokens    = Colls.fromItems(lpId -> lpAmount, tergId -> bootTErg, tusdId -> bootTUsd),
    additionalRegisters = bootRegisters
  )

  val feeAddress = Pay2SAddress(ErgoScriptPredef.feeProposition())
  val feeOut     = new ErgoBoxCandidate(feeNErgs, feeAddress.script, curHeight)

  val change = new ErgoBoxCandidate(
    inputValue - bootNErg - feeNErgs,
    selfAddress.script,
    curHeight,
    additionalTokens =
      Colls.fromItems(tergId -> (tergAmount - bootTErg), tusdId -> (tusdAmount - bootTUsd), edextId -> edextAmount)
  )

  val inputs = Vector(input)
  val outs   = Vector(boot, change, feeOut)
  val utx    = UnsignedErgoLikeTransaction(inputs, outs)
  val tx     = ErgoUnsafeProver.prove(utx, sk)

  println(tx.asJson.noSpacesSortKeys)
}
