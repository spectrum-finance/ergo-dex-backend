package org.ergoplatform.dex.demo

import org.ergoplatform.ErgoBox.{NonMandatoryRegisterId, R4, R5, R6}
import org.ergoplatform.dex.protocol.codecs._
import org.ergoplatform.ergo.syntax._
import org.ergoplatform.wallet.interpreter.ErgoUnsafeProver
import org.ergoplatform.{ErgoBoxCandidate, UnsignedErgoLikeTransaction, UnsignedInput}
import scorex.crypto.authds.ADKey
import scorex.crypto.hash.Digest32
import scorex.util.encode.Base16
import sigmastate.SType.AnyOps
import sigmastate.Values.{Constant, EvaluatedValue}
import sigmastate.eval.Extensions._
import sigmastate.eval._
import sigmastate.{SByte, SCollection, SType}

object IssueAsset extends App with SigmaPlatform {

  val secretHex = ""
  val inputId   = "30974274078845f263b4f21787e33cc99e9ec19a17ad85a5bc6da2cca91c5a2e"

  val input = getInput(inputId).get

  val height = currentHeight()

  val newTokenId       = Digest32 @@ (ADKey !@@ input.boxId.toErgo)
  val newTokenEmission = 4500000L * math.pow(10, 10).toLong

  val TERG  = getToken("f45c4f0d95ce1c64defa607d94717a9a30c00fdd44827504be20db19f4dce36f", input)
  val TUSD  = getToken("f302c6c042ada3566df8e67069f8ac76d10ce15889535141593c168f3c59e776", input)
  val WTERG = getToken("ef802b475c06189fdbf844153cdc1d449a5ba87cce13d11bb47b5a539f27f12b", input)

  val registers = Map(
    R4 -> Constant("WT_ADA".getBytes().toColl.asWrappedType, SCollection(SByte)),
    R5 -> Constant("Wrapped Test ADA".getBytes().toColl.asWrappedType, SCollection(SByte)),
    R6 -> Constant("8".getBytes().toColl.asWrappedType, SCollection(SByte))
  ).asInstanceOf[Map[NonMandatoryRegisterId, EvaluatedValue[SType]]]

  val output = new ErgoBoxCandidate(
    value               = input.value - minerFeeNErg,
    ergoTree            = selfAddress.script,
    creationHeight      = height,
    additionalTokens    = Colls.fromItems(newTokenId -> newTokenEmission, TERG, TUSD, WTERG),
    additionalRegisters = registers
  )

  val inputs0 = Vector(new UnsignedInput(ADKey @@ Base16.decode(inputId).get))
  val outs0   = Vector(output, minerFeeBox)
  val utx0    = UnsignedErgoLikeTransaction(inputs0, outs0)
  val tx0     = ErgoUnsafeProver.prove(utx0, sk)

  println("Submitting init tx")
  val s0 = submitTx(tx0)
  println(s0.map(id => s"Done. $id"))
}

object SendAsset extends App with SigmaPlatform {

  val secretHex = ""
  val inputId   = "5f9c91a806ccf2e55ad70e04ed0e9a66ce61ebc163ca6c15da1a461e4723bc64"

  val input = getInput(inputId).get

  val height = currentHeight()

  val newTokenId       = Digest32 @@ (ADKey !@@ input.boxId.toErgo)
  val newTokenEmission = 4500000L * math.pow(10, 10).toLong

  val TERG  = getToken("f45c4f0d95ce1c64defa607d94717a9a30c00fdd44827504be20db19f4dce36f", input)
  val TUSD  = getToken("f302c6c042ada3566df8e67069f8ac76d10ce15889535141593c168f3c59e776", input)
  val WTERG = getToken("ef802b475c06189fdbf844153cdc1d449a5ba87cce13d11bb47b5a539f27f12b", input)
  val WTADA = getToken("30974274078845f263b4f21787e33cc99e9ec19a17ad85a5bc6da2cca91c5a2e", input)
  val LP = getToken("fffb9895aa02cf55714bae3fe77d7a11ec86d0b28462e2d45d3e6dcfd9a6c11e", input)

  val sendErg = 50L * math.pow(10, 9).toLong
  val sendAda = 100L * math.pow(10, 8).toLong

  val min = 1000000

  val recv = addressEncoder.fromString("9g1N1xqhrNG1b2TkmFcQGTFZ47EquUYUZAiWWCBEbZaBcsMhXJU").get

  val output = new ErgoBoxCandidate(
    value               = min,
    ergoTree            = recv.script,
    creationHeight      = height,
    additionalTokens    = Colls.fromItems(WTERG._1 -> sendErg, WTADA._1 -> sendAda)
  )

  val change = new ErgoBoxCandidate(
    value               = input.value - minerFeeNErg - min,
    ergoTree            = selfAddress.script,
    creationHeight      = height,
    additionalTokens    = Colls.fromItems(TERG, TUSD, WTERG._1 -> (WTERG._2 - sendErg), WTADA._1 -> (WTADA._2 - sendAda))
  )

  val inputs0 = Vector(new UnsignedInput(ADKey @@ Base16.decode(inputId).get))
  val outs0   = Vector(output, change, minerFeeBox)
  val utx0    = UnsignedErgoLikeTransaction(inputs0, outs0)
  val tx0     = ErgoUnsafeProver.prove(utx0, sk)

  println("Submitting init tx")
  val s0 = submitTx(tx0)
  println(s0.map(id => s"Done. $id"))
}
