package org.ergoplatform.dex.demo

import org.ergoplatform.ErgoBox._
import org.ergoplatform._
import org.ergoplatform.dex.protocol.codecs._
import org.ergoplatform.dex.protocol.sigmaUtils
import org.ergoplatform.dex.sources.{n2tContracts, t2tContracts}
import org.ergoplatform.ergo.syntax._
import org.ergoplatform.wallet.interpreter.ErgoUnsafeProver
import scorex.crypto.authds.ADKey
import scorex.crypto.hash.Digest32
import scorex.util.encode.Base16
import sigmastate.SType
import sigmastate.Values.{ByteArrayConstant, ErgoTree, EvaluatedValue, IntConstant}
import sigmastate.eval._
import sigmastate.lang.Terms.ValueOps

import java.math.BigInteger

object Utils {

  def sqrt(x: BigInteger): BigInteger = {
    var div  = BigInteger.ZERO.setBit(x.bitLength / 2)
    var div2 = div
    // Loop until we hit the same value twice in a row, or wind
    // up alternating.

    while (true) {
      val y = div.add(x.divide(div)).shiftRight(1)
      if (y.equals(div) || y.equals(div2)) return y
      div2 = div
      div  = y
    }
    throw new Error("SQRT failed")
  }
}

import Utils._

object CreateCfmmPool extends App with SigmaPlatform {

  val secretHex = ""

  val inputsIds =
    "ef8b8067973e3f96e3b39d54a9b53039414986392b1861ed572db67ac96f7f60" ::
    "87ff47c5a9b0a60b4f7ec8a1c12ce5fcd104156ee2d9ba0d0e12f1e4a080a678" ::
    "df9e09e778e714b51bfb0a98d8517a855b58f08c263be5759d0f2cd1ad83bc50" :: Nil

  val inputs       = inputsIds.map(inputId => getInput(inputId).get)
  val totalNErgsIn = inputs.map(_.value).sum
  val tokensIn     = extractTokens(inputs)

  val lpId       = Digest32 @@ (ADKey !@@ inputs(0).boxId.toErgo)
  val burnLP     = 1000
  val emissionLP = Long.MaxValue - burnLP

  val lpName = "SigUSD_SigRSV_LP"
  val lpDesc = "SigUSD/SigRSV pool LP tokens"

  val lockNErgs = 4000000L
  val minValue  = 500000L

  val bootInputNErg = minerFeeNErg + lockNErgs + minValue

  val depositSigUSD = 10000L // 100
  val depositSigRSV = 15600L // 15600

  val SigUSD = getToken("03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04", inputs)
  val SigRSV = getToken("003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0", inputs)

  require(SigUSD._2 >= depositSigUSD)
  require(SigRSV._2 >= depositSigRSV)

  val height = currentHeight()

  // Init TX

  val boot = new ErgoBoxCandidate(
    value            = bootInputNErg,
    ergoTree         = selfAddress.script,
    creationHeight   = height,
    additionalTokens = Colls.fromItems(lpId -> emissionLP, SigUSD._1 -> depositSigUSD, SigRSV._1 -> depositSigRSV),
    additionalRegisters = scala.Predef.Map(
      R4 -> ByteArrayConstant(lpName.getBytes()),
      R5 -> ByteArrayConstant(lpDesc.getBytes()),
      R6 -> ByteArrayConstant("0".getBytes())
    )
  )

  val change0 = new ErgoBoxCandidate(
    value          = totalNErgsIn - bootInputNErg - minerFeeNErg,
    ergoTree       = selfAddress.script,
    creationHeight = height,
    additionalTokens = Colls.fromItems(tokensIn.filterNot { case (tid, _) =>
      java.util.Arrays.equals(tid, SigUSD._1) || java.util.Arrays.equals(tid, SigRSV._1)
    }: _*)
  )

  val inputs0 = inputs.map(input => new UnsignedInput(ADKey @@ Base16.decode(input.boxId.value).get)).toVector
  val outs0   = Vector(boot, change0, minerFeeBox)
  val utx0    = UnsignedErgoLikeTransaction(inputs0, outs0)
  val tx0     = ErgoUnsafeProver.prove(utx0, sk)

  // Pool TX

  val shareLP = math.sqrt((depositSigUSD * depositSigRSV).toDouble).toLong

  require(shareLP * shareLP <= depositSigUSD * depositSigRSV)

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
    sigmaUtils.updateVersionHeader(ErgoTree.fromProposition(sigma.compile(Map.empty, t2tContracts.pool).asSigmaProp))

  val poolOut = new ErgoBoxCandidate(
    value          = lockNErgs,
    ergoTree       = poolTree,
    creationHeight = height,
    additionalTokens = Colls.fromItems(
      poolNFT   -> 1L,
      lpId      -> (emissionLP - shareLP),
      SigUSD._1 -> depositSigUSD,
      SigRSV._1 -> depositSigRSV
    ),
    additionalRegisters = poolRegisters
  )

  val inputs1 = Vector(new UnsignedInput(bootBox.id))
  val outs1   = Vector(poolOut, lpOut, minerFeeBox)
  val utx1    = UnsignedErgoLikeTransaction(inputs1, outs1)
  val tx1     = ErgoUnsafeProver.prove(utx1, sk)

  println("Submitting init tx")
  val s0 = submitTx(tx0)
  println(s0.map(id => s"Done. $id"))
  println("Submitting pool tx")
  val s1 = submitTx(tx1)
  println(s1.map(id => s"Done. $id"))
}

object CreateNativeCfmmPool extends App with SigmaPlatform {

  val secretHex = ""

  val inputsIds =
    "c95645836ca45f6923978e175b93305185406aa939b213c96e44b6645911d04f" ::
    "06114d23a666883439f8b6121c1ade36e4ece75a18324a86f1ddf4ede8460de2" :: Nil

  val inputs       = inputsIds.map(inputId => getInput(inputId).get)
  val totalNErgsIn = inputs.map(_.value).sum
  val tokensIn     = extractTokens(inputs)

  val lpId       = Digest32 @@ (ADKey !@@ inputs(0).boxId.toErgo)
  val burnLP     = 1000
  val emissionLP = Long.MaxValue - burnLP

  val lpName = "ERG_Erdoge_LP"
  val lpDesc = "ERG/Erdoge pool LP tokens"

  val lockNErgs = 10000000L
  val minValue  = 500000L

  val depositNErgs  = 500000000L
  val depositSigRSV = 1400L

  val bootInputNErg = minerFeeNErg + lockNErgs + minValue + depositNErgs

  val SigRSV = getToken("36aba4b4a97b65be491cf9f5ca57b5408b0da8d0194f30ec8330d1e8946161c1", inputs)

  val height = currentHeight()

  // Init TX

  val boot = new ErgoBoxCandidate(
    value            = bootInputNErg,
    ergoTree         = selfAddress.script,
    creationHeight   = height,
    additionalTokens = Colls.fromItems(lpId -> emissionLP, SigRSV._1 -> depositSigRSV),
    additionalRegisters = scala.Predef.Map(
      R4 -> ByteArrayConstant(lpName.getBytes()),
      R5 -> ByteArrayConstant(lpDesc.getBytes()),
      R6 -> ByteArrayConstant("0".getBytes())
    )
  )

  val change0 = new ErgoBoxCandidate(
    value          = totalNErgsIn - bootInputNErg - minerFeeNErg,
    ergoTree       = selfAddress.script,
    creationHeight = height,
    additionalTokens = Colls.fromItems(tokensIn.filterNot { case (tid, _) =>
      java.util.Arrays.equals(tid, SigRSV._1)
    }: _*)
  )

  val inputs0 = inputs.map(input => new UnsignedInput(ADKey @@ Base16.decode(input.boxId.value).get)).toVector
  val outs0   = Vector(boot, change0, minerFeeBox)
  val utx0    = UnsignedErgoLikeTransaction(inputs0, outs0)
  val tx0     = ErgoUnsafeProver.prove(utx0, sk)

  // Pool TX

  val shareLP = sqrt(((BigInt(depositNErgs) + lockNErgs) * depositSigRSV).bigInteger).toLong

  require(shareLP * shareLP <= (depositNErgs + lockNErgs) * depositSigRSV)

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
    sigmaUtils.updateVersionHeader(ErgoTree.fromProposition(sigma.compile(Map.empty, n2tContracts.pool).asSigmaProp))

  val poolOut = new ErgoBoxCandidate(
    value          = depositNErgs + lockNErgs,
    ergoTree       = poolTree,
    creationHeight = height,
    additionalTokens = Colls.fromItems(
      poolNFT   -> 1L,
      lpId      -> (emissionLP - shareLP),
      SigRSV._1 -> depositSigRSV
    ),
    additionalRegisters = poolRegisters
  )

  val inputs1 = Vector(new UnsignedInput(bootBox.id))
  val outs1   = Vector(poolOut, lpOut, minerFeeBox)
  val utx1    = UnsignedErgoLikeTransaction(inputs1, outs1)
  val tx1     = ErgoUnsafeProver.prove(utx1, sk)

  println("Submitting init tx")
  val s0 = submitTx(tx0)
  println(s0.map(id => s"Done. $id"))
  println("Submitting pool tx")
  val s1 = submitTx(tx1)
  println(s1.map(id => s"Done. $id"))
}
