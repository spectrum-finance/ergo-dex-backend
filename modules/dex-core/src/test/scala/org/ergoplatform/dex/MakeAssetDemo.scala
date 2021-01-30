package org.ergoplatform.dex

import org.ergoplatform.ErgoBox.{R4, R5, R6}
import org.ergoplatform._
import org.ergoplatform.wallet.interpreter.ErgoUnsafeProver
import org.ergoplatform.dex.protocol.codecs._
import scorex.crypto.authds.ADKey
import scorex.crypto.hash.Digest32
import scorex.util.encode.Base16
import sigmastate.Values.ByteArrayConstant
import sigmastate.basics.DLogProtocol.DLogProverInput
import sigmastate.eval._
import io.circe.syntax._
import org.bouncycastle.util.BigIntegers

object MakeAssetDemo extends App {

  val secret     = ""
  val inputId    = "7c232b68665d233356e9abadf3820abff725105c5ccfa8618b77bc3a8bf603ce"
  val inputValue = 1947000000L
  val recvAddr   = "9hWMWtGho2VBPsSRigWMUUtk9sWWPFKSdDWcxSvV9TiTB4PCRKc"
  val curHeight  = 415703

  implicit val e: ErgoAddressEncoder = ErgoAddressEncoder(ErgoAddressEncoder.MainnetNetworkPrefix)

  val sk          = DLogProverInput(BigIntegers.fromUnsignedByteArray(Base16.decode(secret).get))
  val pk          = sk.publicImage
  val selfAddress = P2PKAddress(pk)
  val recvAddress = e.fromString(recvAddr).get.asInstanceOf[P2PKAddress]

  val input = new UnsignedInput(ADKey @@ Base16.decode(inputId).get)

  val tokenId          = Digest32 @@ (ADKey !@@ input.boxId)
  val tokenAmount      = 100L
  val tokenName        = "EDEXTT"
  val tokenDescription = "Ergo DEX Test Token"
  val tokenDecimals    = 0

  val tokenEip4Registers = scala.Predef.Map(
    R4 -> ByteArrayConstant(tokenName.getBytes("UTF-8")),
    R5 -> ByteArrayConstant(tokenDescription.getBytes("UTF-8")),
    R6 -> ByteArrayConstant(String.valueOf(tokenDecimals).getBytes("UTF-8"))
  )

  val out1 = new ErgoBoxCandidate(
    1000000000L,
    recvAddress.script,
    curHeight
  )

  val feeAddress = Pay2SAddress(ErgoScriptPredef.feeProposition())
  val feeOut     = new ErgoBoxCandidate(1000000L, feeAddress.script, curHeight)

  val out2 = new ErgoBoxCandidate(
    inputValue - feeOut.value,
    selfAddress.script,
    curHeight,
    additionalTokens    = Colls.fromItems(tokenId -> tokenAmount),
    additionalRegisters = tokenEip4Registers
  )

  val inputs = Vector(input)
  val outs   = Vector(out1, out2, feeOut)
  val utx    = UnsignedErgoLikeTransaction(inputs, outs)
  val tx     = ErgoUnsafeProver.prove(utx, sk)

  println(tx.asJson.noSpacesSortKeys)
}
