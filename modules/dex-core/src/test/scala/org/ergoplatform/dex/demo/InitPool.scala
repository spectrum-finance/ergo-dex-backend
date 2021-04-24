package org.ergoplatform.dex.demo

import io.circe.syntax._
import org.bouncycastle.util.BigIntegers
import org.ergoplatform.ErgoBox.{NonMandatoryRegisterId, R4}
import org.ergoplatform._
import org.ergoplatform.dex.protocol.codecs._
import org.ergoplatform.wallet.interpreter.ErgoUnsafeProver
import scorex.crypto.authds.ADKey
import scorex.crypto.hash.{Blake2b256, Digest32}
import scorex.util.encode.Base16
import sigmastate.Values.{ErgoTree, LongConstant}
import sigmastate.basics.DLogProtocol.DLogProverInput
import sigmastate.eval.{IRContext, _}
import sigmastate.lang.SigmaCompiler
import sigmastate.lang.Terms.ValueOps

object InitPool extends App {

  val secret     = ""
  val inputId    = "d7d0d6ecc4ed6c282a22655da253070157b381173460a6a8f556eace1529791e"
  val inputValue = 4000000L
  val curHeight  = 473151

  implicit val IR: IRContext         = new CompiletimeIRContext()
  implicit val e: ErgoAddressEncoder = ErgoAddressEncoder(ErgoAddressEncoder.MainnetNetworkPrefix)
  val sigma                          = SigmaCompiler(ErgoAddressEncoder.MainnetNetworkPrefix)

  val sk          = DLogProverInput(BigIntegers.fromUnsignedByteArray(Base16.decode(secret).get))
  val pk          = sk.publicImage
  val selfAddress = P2PKAddress(pk)

  val input = new UnsignedInput(ADKey @@ Base16.decode(inputId).get)

  val lpId   = Digest32 @@ (Base16.decode("dbd9424f420cc104eb4cdddb3ffb1e67f60900c1c23eed73b02dce87e70ff327").get)
  val tergId = Digest32 @@ (Base16.decode("f45c4f0d95ce1c64defa607d94717a9a30c00fdd44827504be20db19f4dce36f").get)
  val tusdId = Digest32 @@ (Base16.decode("f302c6c042ada3566df8e67069f8ac76d10ce15889535141593c168f3c59e776").get)

  val nftId   = Digest32 @@ (ADKey !@@ input.boxId)
  val nftName = "TERG_TUSD_NFT"

  val poolScript = ErgoTree.fromProposition(sigma.compile(Map.empty, contracts.poolContract).asSigmaProp)
  val poolSH     = Blake2b256.hash(poolScript.bytes)

  val bootNErg = 2000000L

  val bootTErg = 1000000000L
  val bootTUsd = 100000000000L

  val bootLP = 1000000000000000000L

  val feeNErg = 1000000L

  require(bootTErg * bootTUsd <= Long.MaxValue)
  val sharesLP = math.sqrt(bootTErg * bootTUsd).toLong
  println(sharesLP)
  println(math.sqrt(sharesLP * sharesLP).toLong)
  require(sharesLP * sharesLP <= bootTErg * bootTUsd)

  val poolFee = 995L
  val poolLP  = bootLP - sharesLP

  val bootRegisters = scala.Predef.Map(
    (R4: NonMandatoryRegisterId) -> LongConstant(poolFee)
  )

  val pool = new ErgoBoxCandidate(
    bootNErg,
    poolScript,
    curHeight,
    additionalTokens    = Colls.fromItems(nftId -> 1L, lpId -> poolLP, tergId -> bootTErg, tusdId -> bootTUsd),
    additionalRegisters = bootRegisters
  )

  val feeAddress = Pay2SAddress(ErgoScriptPredef.feeProposition())
  val feeOut     = new ErgoBoxCandidate(feeNErg, feeAddress.script, curHeight)

  val reward = new ErgoBoxCandidate(
    inputValue - bootNErg - feeNErg,
    selfAddress.script,
    curHeight,
    additionalTokens = Colls.fromItems(lpId -> sharesLP)
  )

  val inputs = Vector(input)
  val outs   = Vector(pool, reward, feeOut)
  val utx    = UnsignedErgoLikeTransaction(inputs, outs)
  val tx     = ErgoUnsafeProver.prove(utx, sk)

  println(tx.asJson.noSpacesSortKeys)
}
