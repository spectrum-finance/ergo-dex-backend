package org.ergoplatform.dex.demo

import org.bouncycastle.util.BigIntegers
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
import sigmastate.basics.DLogProtocol
import sigmastate.basics.DLogProtocol.DLogProverInput
import sigmastate.eval._
import sigmastate.lang.SigmaCompiler
import sigmastate.lang.Terms.ValueOps
import io.circe.syntax._
import org.ergoplatform._
import org.ergoplatform.dex.demo.CreateNativeCfmmPool.{tx0, tx1}

import java.math.BigInteger
import java.security.SecureRandom

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

// todo: Create wallet here
object GenWallet extends App {
  implicit def IR: IRContext                      = new CompiletimeIRContext()
  implicit def addressEncoder: ErgoAddressEncoder = ErgoAddressEncoder(ErgoAddressEncoder.MainnetNetworkPrefix)
  def sigma: SigmaCompiler                        = SigmaCompiler(ErgoAddressEncoder.MainnetNetworkPrefix)

  val rnd = new SecureRandom()

  def genSecret(): Array[Byte] = scorex.util.Random.randomBytes(32)

  val secret                         = genSecret()
  val sk: DLogProverInput            = DLogProverInput(BigIntegers.fromUnsignedByteArray(genSecret()))
  def selfPk: DLogProtocol.ProveDlog = sk.publicImage
  def selfAddress: P2PKAddress       = P2PKAddress(selfPk)

  println(s"SECRET : ${Base16.encode(sk.w.toByteArray)}") // todo: Save this key in a safe place
  println(s"ADDRESS: ${addressEncoder.toString(selfAddress)}")
}

object CreateCfmmPool extends App with SigmaPlatform {

  val secretHex = "" // todo: Paste your secret in HEX here

  val inputsIds = List() // todo: Paste your inputs here

  val inputs       = inputsIds.map(inputId => getInput(inputId).get)
  val totalNErgsIn = inputs.map(_.value).sum
  val tokensIn     = extractTokens(inputs)

  val lpId       = Digest32 @@ (ADKey !@@ inputs(0).boxId.toErgo)
  val burnLP     = 1000
  val emissionLP = Long.MaxValue - burnLP

  val lpName = "Ergopad_SigUSD_LP"
  val lpDesc = "Ergopad/SigUSD pool LP tokens"

  val lockNErgs = 4000000L
  val minValue  = 500000L

  val bootInputNErg = minerFeeNErg + lockNErgs + minValue

  val depositSigUSD = 25000L // 100  // todo: Adjust deposit amount
  val depositSigRSV = 1000L // 15600 // todo: Adjust deposit amount

  val Ergopad = getToken("d71693c49a84fbbecd4908c94813b46514b18b67a99952dc1e6e4791556de413", inputs)
  val SigUSD  = getToken("03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04", inputs)

  require(Ergopad._2 >= depositSigUSD)
  require(SigUSD._2 >= depositSigRSV)

  val height = currentHeight()

  // Init TX

  val boot = new ErgoBoxCandidate(
    value            = bootInputNErg,
    ergoTree         = selfAddress.script,
    creationHeight   = height,
    additionalTokens = Colls.fromItems(lpId -> emissionLP, Ergopad._1 -> depositSigUSD, SigUSD._1 -> depositSigRSV),
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
      java.util.Arrays.equals(tid, Ergopad._1) || java.util.Arrays.equals(tid, SigUSD._1)
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

  val poolFeeNum = 996
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
      poolNFT    -> 1L,
      lpId       -> (emissionLP - shareLP),
      Ergopad._1 -> depositSigUSD,
      SigUSD._1  -> depositSigRSV
    ),
    additionalRegisters = poolRegisters
  )

  val inputs1 = Vector(new UnsignedInput(bootBox.id))
  val outs1   = Vector(poolOut, lpOut, minerFeeBox)
  val utx1    = UnsignedErgoLikeTransaction(inputs1, outs1)
  val tx1     = ErgoUnsafeProver.prove(utx1, sk)

  // todo: First check the resulting transactions:
  println("Init TX:")
  println(tx0.asJson.spaces2SortKeys)
  println()
  println("Pool TX:")
  println(tx1.asJson.spaces2SortKeys)

  // todo: Then uncomment this:
//  println("Submitting init tx")
//  val s0 = submitTx(tx0)
//  println(s0.map(id => s"Done. $id"))
//  println("Submitting pool tx")
//  val s1 = submitTx(tx1)
//  println(s1.map(id => s"Done. $id"))
}

object CreateNativeCfmmPool extends App with SigmaPlatform {

  val secretHex = "" // todo: Paste your secret in HEX here

  val inputsIds = List() // todo: Paste your inputs here

  val inputs       = inputsIds.map(inputId => getInput(inputId).get)
  val totalNErgsIn = inputs.map(_.value).sum
  val tokensIn     = extractTokens(inputs)

  val lpId       = Digest32 @@ (ADKey !@@ inputs(0).boxId.toErgo)
  val burnLP     = 1000
  val emissionLP = Long.MaxValue - burnLP

  val lpName = "ERG_Ergopad_LP"
  val lpDesc = "ERG/Ergopad pool LP tokens"

  val lockNErgs = 0L
  val minValue  = 500000L

  val depositNErgs  = 2906976740L // todo: Adjust deposit amount
  val depositSigRSV = 25000L // todo: Adjust deposit amount

  val bootInputNErg = minerFeeNErg + lockNErgs + minValue + depositNErgs

  val Ergopad = getToken("d71693c49a84fbbecd4908c94813b46514b18b67a99952dc1e6e4791556de413", inputs)

  val height = currentHeight()

  // Init TX

  val boot = new ErgoBoxCandidate(
    value            = bootInputNErg,
    ergoTree         = selfAddress.script,
    creationHeight   = height,
    additionalTokens = Colls.fromItems(lpId -> emissionLP, Ergopad._1 -> depositSigRSV),
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
      java.util.Arrays.equals(tid, Ergopad._1)
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

  val poolFeeNum = 996
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
      poolNFT    -> 1L,
      lpId       -> (emissionLP - shareLP),
      Ergopad._1 -> depositSigRSV
    ),
    additionalRegisters = poolRegisters
  )

  val inputs1 = Vector(new UnsignedInput(bootBox.id))
  val outs1   = Vector(poolOut, lpOut, minerFeeBox)
  val utx1    = UnsignedErgoLikeTransaction(inputs1, outs1)
  val tx1     = ErgoUnsafeProver.prove(utx1, sk)

  // todo: First check the resulting transactions:
  println("Init TX:")
  println(tx0.asJson.spaces2SortKeys)
  println()
  println("Pool TX:")
  println(tx1.asJson.spaces2SortKeys)

  // todo: Then uncomment this:
//  println("Submitting init tx")
//  val s0 = submitTx(tx0)
//  println(s0.map(id => s"Done. $id"))
//  println("Submitting pool tx")
//  val s1 = submitTx(tx1)
//  println(s1.map(id => s"Done. $id"))
}
