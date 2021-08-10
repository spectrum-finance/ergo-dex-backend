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
  val inputId   = "6ff3c5226abb8a6330fca4c85d761e26797a3333bbbcda013ccbf01ee8f3b924"

  val recvs = Seq(
    "9gBCM4jyoy31VzUogBZkJwVyZwu9L4cbfzjPamieUSFHV6FVjk3",
    "9g1N1xqhrNG1b2TkmFcQGTFZ47EquUYUZAiWWCBEbZaBcsMhXJU"
  ).map(recv => addressEncoder.fromString(recv).get)

  val input = getInput(inputId).get

  val height = currentHeight()

  val newTokenId       = Digest32 @@ (ADKey !@@ input.boxId.toErgo)
  val newTokenEmission = 4500000L * math.pow(10, 10).toLong

  val WTERG0 = getToken("ef802b475c06189fdbf844153cdc1d449a5ba87cce13d11bb47b5a539f27f12b", input)
  val WTADA0 = getToken("30974274078845f263b4f21787e33cc99e9ec19a17ad85a5bc6da2cca91c5a2e", input)

  val otherTokens0 = extractTokens(input).filterNot { case (id, _) =>
    java.util.Arrays.equals(id, WTERG0._1) || java.util.Arrays.equals(id, WTADA0._1)
  }

  val sendErg = 1000L * math.pow(10, 9).toLong
  val sendAda = 1000L * math.pow(10, 8).toLong

  val min = 100000L

  val outputs = recvs.map { recv =>
    new ErgoBoxCandidate(
      value            = min,
      ergoTree         = recv.script,
      creationHeight   = height,
      additionalTokens = Colls.fromItems(WTERG0._1 -> sendErg, WTADA0._1 -> sendAda)
    )
  }

  val change = new ErgoBoxCandidate(
    value          = input.value - minerFeeNErg - min * recvs.size,
    ergoTree       = selfAddress.script,
    creationHeight = height,
    additionalTokens = Colls.fromItems(
      (otherTokens0 ++ Seq(
        WTERG0._1 -> (WTERG0._2 - sendErg * recvs.size),
        WTADA0._1 -> (WTADA0._2 - sendAda * recvs.size)
      )): _*
    )
  )

  val inputs0 = Vector(new UnsignedInput(ADKey @@ Base16.decode(inputId).get))
  val outs0   = Vector(change, minerFeeBox) ++ outputs
  val utx0    = UnsignedErgoLikeTransaction(inputs0, outs0)
  val tx0     = ErgoUnsafeProver.prove(utx0, sk)

  println("Submitting init tx")
  val s0 = submitTx(tx0)
  println(s0.map(id => s"Done. $id"))
}
