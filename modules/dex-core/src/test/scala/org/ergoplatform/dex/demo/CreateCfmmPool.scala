package org.ergoplatform.dex.demo

import org.ergoplatform.ErgoBox._
import org.ergoplatform._
import org.ergoplatform.ergo.syntax._
import org.ergoplatform.dex.protocol.codecs._
import org.ergoplatform.dex.protocol.sigmaUtils
import org.ergoplatform.wallet.interpreter.ErgoUnsafeProver
import scorex.crypto.authds.ADKey
import scorex.crypto.hash.Digest32
import scorex.util.encode.Base16
import sigmastate.SType
import sigmastate.Values.{ByteArrayConstant, ErgoTree, EvaluatedValue, IntConstant}
import sigmastate.eval._
import sigmastate.lang.Terms.ValueOps

object CreateCfmmPool extends App with SigmaPlatform {

  val secretHex = ""
  val inputId   = "7f14228a5fd5b5c5d74bfbced3491916e2dc305106dd043f78b65b4cced9c2b9"

  val input = getInput(inputId).get

  val lpId       = Digest32 @@ (ADKey !@@ input.boxId.toErgo)
  val emissionLP = Long.MaxValue
  val lpName     = "TERG_TUSD_LP"

  val lockNErgs = 4000000L
  val feeNErgs  = 1000000L
  val minValue  = 500000L

  val bootInputNErg = feeNErgs + lockNErgs + minValue

  require(input.value >= bootInputNErg + feeNErgs)

  val depositTErg = 10000000000L
  val depositTUsd = 1000000000000L

  val TERG = getToken("f45c4f0d95ce1c64defa607d94717a9a30c00fdd44827504be20db19f4dce36f", input)
  val TUSD = getToken("f302c6c042ada3566df8e67069f8ac76d10ce15889535141593c168f3c59e776", input)

  require(TERG._2 >= depositTErg)
  require(TUSD._2 >= depositTUsd)

  val height = currentHeight()

  // Init TX

  val bootRegisters: Map[NonMandatoryRegisterId, _ <: EvaluatedValue[_ <: SType]] =
    scala.Predef.Map(
      R4 -> ByteArrayConstant(lpName.getBytes("UTF-8"))
    )

  val boot = new ErgoBoxCandidate(
    value               = bootInputNErg,
    ergoTree            = selfAddress.script,
    creationHeight      = height,
    additionalTokens    = Colls.fromItems(lpId -> emissionLP, TERG._1 -> depositTErg, TUSD._1 -> depositTUsd),
    additionalRegisters = bootRegisters
  )

  val feeAddress = Pay2SAddress(ErgoScriptPredef.feeProposition())

  val feeOut0 = new ErgoBoxCandidate(
    value          = feeNErgs,
    ergoTree       = feeAddress.script,
    creationHeight = height
  )

  val change0 = new ErgoBoxCandidate(
    value            = input.value - bootInputNErg - feeNErgs,
    ergoTree         = selfAddress.script,
    creationHeight   = height,
    additionalTokens = Colls.fromItems(TERG._1 -> (TERG._2 - depositTErg), TUSD._1 -> (TUSD._2 - depositTUsd))
  )

  val inputs0 = Vector(new UnsignedInput(ADKey @@ Base16.decode(inputId).get))
  val outs0   = Vector(boot, change0, feeOut0)
  val utx0    = UnsignedErgoLikeTransaction(inputs0, outs0)
  val tx0     = ErgoUnsafeProver.prove(utx0, sk)

  // Pool TX

  val shareLP = math.sqrt((depositTErg * depositTUsd).toDouble).toLong

  require(shareLP * shareLP <= depositTErg * depositTUsd)

  val lpOut = new ErgoBoxCandidate(
    value            = minValue,
    ergoTree         = selfAddress.script,
    creationHeight   = height,
    additionalTokens = Colls.fromItems(lpId -> shareLP)
  )

  val bootBox = tx0.outputs(0)

  val poolFeeNum = 995
  val poolNFT    = Digest32 @@ (ADKey !@@ bootBox.id)

  val poolRegisters: Map[NonMandatoryRegisterId, _ <: EvaluatedValue[_ <: SType]] =
    scala.Predef.Map(
      R4 -> IntConstant(poolFeeNum)
    )

  val poolTree =
    sigmaUtils.updateVersionHeader(ErgoTree.fromProposition(sigma.compile(Map.empty, contracts.pool).asSigmaProp))

  val poolOut = new ErgoBoxCandidate(
    value          = lockNErgs,
    ergoTree       = poolTree,
    creationHeight = height,
    additionalTokens = Colls.fromItems(
      poolNFT -> 1L,
      lpId    -> (emissionLP - shareLP),
      TERG._1 -> depositTErg,
      TUSD._1 -> depositTUsd
    ),
    additionalRegisters = poolRegisters
  )

  val inputs1 = Vector(new UnsignedInput(bootBox.id))
  val outs1   = Vector(poolOut, lpOut, feeOut0)
  val utx1    = UnsignedErgoLikeTransaction(inputs1, outs1)
  val tx1     = ErgoUnsafeProver.prove(utx1, sk)

  println("Submitting init tx")
  val s0 = submitTx(tx0)
  println(s0.map(id => s"Done. $id"))
  println("Submitting pool tx")
  val s1 = submitTx(tx1)
  println(s1.map(id => s"Done. $id"))
}
