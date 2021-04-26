package org.ergoplatform.dex.demo

import io.circe.syntax._
import org.bouncycastle.util.BigIntegers
import org.ergoplatform.ErgoBox._
import org.ergoplatform._
import org.ergoplatform.contracts.{DexLimitOrderContracts, DexSellerContractParameters}
import org.ergoplatform.dex.protocol.codecs._
import org.ergoplatform.wallet.interpreter.ErgoUnsafeProver
import scorex.crypto.authds.ADKey
import scorex.crypto.hash.Digest32
import scorex.util.encode.Base16
import sigmastate.SType.AnyOps
import sigmastate.Values.{Constant, EvaluatedValue}
import sigmastate.basics.DLogProtocol.DLogProverInput
import sigmastate.eval.Extensions._
import sigmastate.eval._
import sigmastate.{SByte, SCollection, SLong, SType}

object MakeSellOrder extends App {

  val secret     = ""
  val inputId    = "4773ca4e728f5e0008ca8b74e610a8ba7e335629a5189ff3552cc9625aaba4ee"
  val inputValue = 1904000000L
  val curHeight  = 417945

  val tokenId           = "7c232b68665d233356e9abadf3820abff725105c5ccfa8618b77bc3a8bf603ce"
  val tokenAmount       = 92L
  val tokenAmountToSell = 4L
  val tokenPrice        = 20000000L
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

  val dexRegisters = Map(
    R4 -> Constant(tokenIdNative.toColl.asWrappedType, SCollection(SByte)),
    R5 -> Constant(tokenPrice.asWrappedType, SLong),
    R6 -> Constant(dexFeePerToken.asWrappedType, SLong)
  ).asInstanceOf[Map[NonMandatoryRegisterId, EvaluatedValue[SType]]]

  val dexOut = new ErgoBoxCandidate(
    dexOutValue,
    contract.ergoTree,
    curHeight,
    additionalTokens    = Colls.fromItems(tokenIdNative -> tokenAmountToSell),
    additionalRegisters = dexRegisters
  )

  val feeAddress = Pay2SAddress(ErgoScriptPredef.feeProposition())
  val feeOut     = new ErgoBoxCandidate(1000000L, feeAddress.script, curHeight)

  val change = inputValue - (dexOut.value + feeOut.value)

  val changeOut = new ErgoBoxCandidate(
    change,
    address.script,
    curHeight,
    additionalTokens = Colls.fromItems(tokenIdNative -> (tokenAmount - tokenAmountToSell))
  )

  val inputs = Vector(input)
  val outs   = Vector(dexOut, feeOut, changeOut)
  val utx    = UnsignedErgoLikeTransaction(inputs, outs)
  val tx     = ErgoUnsafeProver.prove(utx, sk)

  println(tx.asJson.noSpacesSortKeys)
}
