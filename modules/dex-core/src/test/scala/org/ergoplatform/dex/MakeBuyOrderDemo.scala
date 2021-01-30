package org.ergoplatform.dex

import io.circe.syntax._
import org.bouncycastle.util.BigIntegers
import org.ergoplatform._
import org.ergoplatform.contracts.{DexBuyerContractParameters, DexLimitOrderContracts}
import org.ergoplatform.dex.protocol.codecs._
import org.ergoplatform.wallet.interpreter.ErgoUnsafeProver
import scorex.crypto.authds.ADKey
import scorex.util.encode.Base16
import sigmastate.basics.DLogProtocol.DLogProverInput

object MakeBuyOrderDemo extends App {

  val secret     = ""
  val inputId    = "0720761aefb161f7d13be97b23291dc6f8a94d03b5aac3bb9e72cbc16da7f48d"
  val inputValue = 1000000000L
  val curHeight  = 415703

  val tokenId        = "7c232b68665d233356e9abadf3820abff725105c5ccfa8618b77bc3a8bf603ce"
  val tokenAmount    = 2
  val tokenPrice     = 210000000L
  val dexFeePerToken = 10000000L

  implicit val e: ErgoAddressEncoder = ErgoAddressEncoder(ErgoAddressEncoder.MainnetNetworkPrefix)

  val sk      = DLogProverInput(BigIntegers.fromUnsignedByteArray(Base16.decode(secret).get))
  val pk      = sk.publicImage
  val address = P2PKAddress(pk)

  val input = new UnsignedInput(ADKey @@ Base16.decode(inputId).get)

  val orderParams =
    DexBuyerContractParameters(pk, Base16.decode(tokenId).get, tokenPrice, dexFeePerToken)
  val contract = DexLimitOrderContracts.buyerContractInstance(orderParams)

  val dexOutValue = (tokenPrice + dexFeePerToken) * tokenAmount
  val dexOut      = new ErgoBoxCandidate(dexOutValue, contract.ergoTree, curHeight)

  val feeAddress = Pay2SAddress(ErgoScriptPredef.feeProposition())
  val feeOut     = new ErgoBoxCandidate(1000000L, feeAddress.script, curHeight)

  val change    = inputValue - (dexOut.value + feeOut.value)
  val changeOut = new ErgoBoxCandidate(change, address.script, curHeight)

  val inputs = Vector(input)
  val outs   = Vector(dexOut, feeOut, changeOut)
  val utx    = UnsignedErgoLikeTransaction(inputs, outs)
  val tx     = ErgoUnsafeProver.prove(utx, sk)

  println(tx.asJson.noSpacesSortKeys)
}
