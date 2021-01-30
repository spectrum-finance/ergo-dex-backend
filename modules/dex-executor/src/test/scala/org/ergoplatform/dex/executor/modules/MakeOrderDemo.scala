package org.ergoplatform.dex.executor.modules

import org.ergoplatform._
import org.ergoplatform.contracts.{DexBuyerContractParameters, DexLimitOrderContracts}
import org.ergoplatform.wallet.interpreter.ErgoUnsafeProver
import scorex.crypto.authds.ADKey
import scorex.util.encode.Base16
import sigmastate.basics.DLogProtocol.DLogProverInput

object MakeOrderDemo extends App {

  val secret         = ""
  val inputId        = "4ef417b69868924ee9cb6fcc73704ff6aba6d9db414ab2eff54b9edb851dd009"
  val inputValue     = 3000000000L
  val tokenId        = "2ae90f3b2e4f8bc32b52f443e4647962c8534d2f61a640ddd5a793619df20c9f"
  val tokenPrice     = 20000000L
  val dexFeePerToken = 10000000L

  implicit val e: ErgoAddressEncoder = ErgoAddressEncoder(ErgoAddressEncoder.MainnetNetworkPrefix)

  val sk      = DLogProverInput(BigInt(Base16.decode(secret).get).bigInteger)
  val pk      = sk.publicImage
  val address = P2PKAddress(pk)

  val input = new UnsignedInput(ADKey @@ Base16.decode(inputId).get)

  val orderParams =
    DexBuyerContractParameters(pk, Base16.decode(tokenId).get, tokenPrice, dexFeePerToken)
  val contract = DexLimitOrderContracts.buyerContractInstance(orderParams)

  val dexOutValue = tokenPrice + dexFeePerToken
  val dexOut      = new ErgoBoxCandidate(dexOutValue, contract.ergoTree, 410109)

  val feeAddress = Pay2SAddress(ErgoScriptPredef.feeProposition())
  val feeOut     = new ErgoBoxCandidate(1000000L, feeAddress.script, 410109)

  val change    = inputValue - (dexOut.value + feeOut.value)
  val changeOut = new ErgoBoxCandidate(change, address.script, 410109)

  val inputs = Vector(input)
  val outs   = Vector(dexOut, feeOut, changeOut)
  val utx    = UnsignedErgoLikeTransaction(inputs, outs)
  val tx     = ErgoUnsafeProver.prove(utx, sk)
}
