package org.ergoplatform.dex.demo

import org.ergoplatform._
import org.ergoplatform.dex.protocol.codecs._
import org.ergoplatform.dex.sources.t2tContracts
import org.ergoplatform.ergo.syntax._
import org.ergoplatform.wallet.interpreter.ErgoUnsafeProver
import scorex.crypto.authds.ADKey
import scorex.util.encode.Base16
import sigmastate.Values.ErgoTree
import sigmastate.eval._
import sigmastate.lang.Terms.ValueOps

object MakeSwapProxy extends App with SigmaPlatform {

  val secretHex = ""
  val inputId   = "a166f1ee13a58150c2dfe3fee6a614647a3cb476a4d26386b9fa4d5fde5cc572"

  val poolInId = "f56db4440333fcd13f8bf8be291afd6db80427c45a0f7aeba034d3dac9a269dc"
  val recvAddr = "9iCzZksco8R2P8HXTsZiFAq2km59PDznuTykBRjHd74BfBG3kk8"

  val curHeight = currentHeight()

  val SafeMinAmountNErg = 400000L

  // User input
  val input       = getInput(inputId).get
  val inputTokens = extractTokens(input)

  // Pool input
  val poolIn     = getInput(poolInId).get
  val poolFeeNum = 996

  val poolProp = ErgoTree.fromProposition(sigma.compile(Map.empty, t2tContracts.pool).asSigmaProp)

  val poolX     = poolIn.assets(2)
  val reservesX = poolX.amount
  val poolY     = poolIn.assets(3)
  val reservesY = poolY.amount

  def inputAmount(xy: Boolean, output: Long) =
    if (xy)
      (BigInt(reservesY) * output * 1000 / ((BigInt(reservesX) - output) * poolFeeNum)) + 1
    else
      (BigInt(reservesX) * output * 1000 / ((BigInt(reservesY) - output) * poolFeeNum)) + 1

  // Order params
  val outX        = 50000000L
  val inY         = inputAmount(xy = true, outX).toLong
  val relaxedOutX = outX + 1L

  val dexFeeNErg = outX / 10

  require(dexFeeNErg >= SafeMinAmountNErg)

  require(BigInt(reservesX) * inY * poolFeeNum <= relaxedOutX * (BigInt(reservesY) * 1000 + inY * poolFeeNum))

  val swapEnv = Map(
    "RedeemerPropBytes" -> Array.fill(32)(7: Byte),
    "PoolNFT"        -> Base16.decode(poolIn.assets(0).tokenId.unwrapped).get,
//    "MinQuoteAmount" -> outX,
    "QuoteId"        -> Base16.decode(poolX.tokenId.unwrapped).get,
    "MinerPropBytes" -> Base16
      .decode(
        "1005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a57304"
      )
      .get,
    "MaxMinerFee" -> 777777L,
    "RefundProp" -> false
  )
  val swapProp = ErgoTree.fromProposition(sigma.compile(swapEnv, t2tContracts.swapV2).asSigmaProp)

  val swap = new ErgoBoxCandidate(
    SafeMinAmountNErg + dexFeeNErg + minerFeeNErg,
    swapProp,
    curHeight,
    additionalTokens = Colls.fromItems(poolY.tokenId.toErgo -> inY)
  )

  val changeTokens: Seq[(ErgoBox.TokenId, Long)] = inputTokens
    .filterNot { case (id, _) =>
      java.util.Arrays.equals(id, poolY.tokenId.toErgo)
    } ++
    Seq((poolY.tokenId.toErgo, inputTokens
      .find { case (id, _) => java.util.Arrays.equals(id, poolY.tokenId.toErgo) }
      .get
      ._2 - inY))

  val change = new ErgoBoxCandidate(
    input.value - swap.value - minerFeeBox.value,
    selfAddress.script,
    curHeight,
    additionalTokens = Colls.fromItems(changeTokens: _*)
  )

  val inputs0 = Vector(new UnsignedInput(ADKey @@ Base16.decode(inputId).get))
  val outs0   = Vector(swap, change, minerFeeBox)
  val utx0    = UnsignedErgoLikeTransaction(inputs0, outs0)
  val tx0: ErgoLikeTransaction = ErgoUnsafeProver.prove(utx0, sk)


  println(s"Submitting init tx: $tx0")
//  val s0 = submitTx(tx0)
//  println(s0.map(id => s"Done. $id"))
}

object ExecuteRefund extends App with SigmaPlatform {
  val secretHex = ""
  val inputId   = "4648a43d26a7cbc08875161694ca524e75e0a2aca1701fbbe0b0a61542f14f52"

  val input       = getInput(inputId).get
  val inputTokens = extractTokens(input)

  val curHeight = currentHeight()

  val refundOut = new ErgoBoxCandidate(
    input.value - minerFeeBox.value,
    selfAddress.script,
    curHeight,
    additionalTokens = Colls.fromItems(inputTokens:_*)
  )

  val inputs0 = Vector(new UnsignedInput(ADKey @@ Base16.decode(inputId).get))
  val outs0   = Vector(refundOut, minerFeeBox)
  val utx0    = UnsignedErgoLikeTransaction(inputs0, outs0)
  val tx0     = ErgoUnsafeProver.prove(utx0, sk)

  println("Submitting init tx")
  val s0 = submitTx(tx0)
  println(s0.map(id => s"Done. $id"))
}
