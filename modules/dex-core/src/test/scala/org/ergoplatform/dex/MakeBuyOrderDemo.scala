package org.ergoplatform.dex

import io.circe.syntax._
import org.bouncycastle.util.BigIntegers
import org.ergoplatform.ErgoBox.{NonMandatoryRegisterId, R4, R5, R6}
import org.ergoplatform._
import org.ergoplatform.contracts.{DexBuyerContractParameters, DexLimitOrderContracts}
import org.ergoplatform.dex.protocol.codecs._
import org.ergoplatform.wallet.interpreter.ErgoUnsafeProver
import scorex.crypto.authds.ADKey
import scorex.crypto.hash.Digest32
import scorex.util.encode.Base16
import sigmastate.SType.AnyOps
import sigmastate.Values.{Constant, EvaluatedValue}
import sigmastate.basics.DLogProtocol.DLogProverInput
import sigmastate.eval.Extensions._
import sigmastate.{SByte, SCollection, SLong, SType}

object MakeBuyOrderDemo extends App {

  val secret     = ""
  val inputId    = "4ef551c6ca686274bab754220a82c093454e79a2534cb64ec7035e7a0cce02b7"
  val inputValue = 123000000L
  val curHeight  = 417949

  val tokenId        = "7c232b68665d233356e9abadf3820abff725105c5ccfa8618b77bc3a8bf603ce"
  val tokenAmount    = 2
  val tokenPrice     = 20000000L
  val dexFeePerToken = 10000000L

  implicit val e: ErgoAddressEncoder = ErgoAddressEncoder(ErgoAddressEncoder.MainnetNetworkPrefix)

  val sk      = DLogProverInput(BigIntegers.fromUnsignedByteArray(Base16.decode(secret).get))
  val pk      = sk.publicImage
  val address = P2PKAddress(pk)

  val input = new UnsignedInput(ADKey @@ Base16.decode(inputId).get)

  val orderParams =
    DexBuyerContractParameters(pk, Base16.decode(tokenId).get, tokenPrice, dexFeePerToken)
  val contract = DexLimitOrderContracts.buyerContractInstance(orderParams)

  val tokenIdNative = Digest32 @@ Base16.decode(tokenId).get

  val dexRegisters = Map(
    R4 -> Constant(tokenIdNative.toColl.asWrappedType, SCollection(SByte)),
    R5 -> Constant(tokenPrice.asWrappedType, SLong),
    R6 -> Constant(dexFeePerToken.asWrappedType, SLong)
  ).asInstanceOf[Map[NonMandatoryRegisterId, EvaluatedValue[SType]]]

  val dexOutValue = (tokenPrice + dexFeePerToken) * tokenAmount
  val dexOut      = new ErgoBoxCandidate(dexOutValue, contract.ergoTree, curHeight, additionalRegisters = dexRegisters)

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
