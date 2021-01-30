package org.ergoplatform.dex

import io.circe.syntax._
import org.bouncycastle.util.BigIntegers
import org.ergoplatform._
import org.ergoplatform.contracts.{DexLimitOrderContracts, DexSellerContractParameters}
import org.ergoplatform.dex.protocol.codecs._
import org.ergoplatform.wallet.interpreter.ErgoUnsafeProver
import scorex.crypto.authds.ADKey
import scorex.crypto.hash.Digest32
import scorex.util.encode.Base16
import sigmastate.basics.DLogProtocol.DLogProverInput
import sigmastate.eval._

object MakeSellOrderDemo extends App {

  val secret     = ""
  val inputId    = "f972e8c9b0fcca5ddfe0b950a76fc57daa304041df2a682f38c2e2a9172a4562"
  val inputValue = 1946000000L
  val curHeight  = 415703

  val tokenId           = "7c232b68665d233356e9abadf3820abff725105c5ccfa8618b77bc3a8bf603ce"
  val tokenAmount       = 100L
  val tokenAmountToSell = 4L
  val tokenPrice        = 200000000L
  val dexFeePerToken    = 5000000L

  implicit val e: ErgoAddressEncoder = ErgoAddressEncoder(ErgoAddressEncoder.MainnetNetworkPrefix)

  val sk      = DLogProverInput(BigIntegers.fromUnsignedByteArray(Base16.decode(secret).get))
  val pk      = sk.publicImage
  val address = P2PKAddress(pk)

  val input = new UnsignedInput(ADKey @@ Base16.decode(inputId).get)

  val orderParams =
    DexSellerContractParameters(pk, Base16.decode(tokenId).get, tokenPrice, dexFeePerToken)
  val contract = DexLimitOrderContracts.sellerContractInstance(orderParams)

  val dexOutValue = tokenAmountToSell * dexFeePerToken

  val tokenIdNative = Digest32 @@ Base16.decode(tokenId).get

  val dexOut = new ErgoBoxCandidate(
    dexOutValue,
    contract.ergoTree,
    curHeight,
    additionalTokens = Colls.fromItems(tokenIdNative -> tokenAmountToSell)
  )

  val feeAddress = Pay2SAddress(ErgoScriptPredef.feeProposition())
  val feeOut     = new ErgoBoxCandidate(1000000L, feeAddress.script, curHeight)

  val change    = inputValue - (dexOut.value + feeOut.value)
  val changeOut = new ErgoBoxCandidate(change, address.script, curHeight,
    additionalTokens = Colls.fromItems(tokenIdNative -> (tokenAmount - tokenAmountToSell)))

  val inputs = Vector(input)
  val outs   = Vector(dexOut, feeOut, changeOut)
  val utx    = UnsignedErgoLikeTransaction(inputs, outs)
  val tx     = ErgoUnsafeProver.prove(utx, sk)

  println(tx.asJson.noSpacesSortKeys)
}
