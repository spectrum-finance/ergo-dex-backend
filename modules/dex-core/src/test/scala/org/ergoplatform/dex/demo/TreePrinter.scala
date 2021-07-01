package org.ergoplatform.dex.demo

import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.protocol.{sigmaUtils, ErgoTreeSerializer}
import org.ergoplatform.ergo.ErgoTreeTemplate
import scorex.crypto.hash.Sha256
import sigmastate.Values.ErgoTree
import sigmastate.basics.DLogProtocol.DLogProverInput
import sigmastate.eval.{CompiletimeIRContext, IRContext}
import sigmastate.lang.SigmaCompiler
import sigmastate.lang.Terms.ValueOps

object TreePrinter extends App {

  import contracts._

  private val dummyPk       = DLogProverInput(BigInt(Long.MaxValue).bigInteger).publicImage
  private val dummyDigest32 = Array.fill(32)(0: Byte)
  private val dummyLong     = 1000L

  implicit private val IR: IRContext = new CompiletimeIRContext()
  val sigma                          = SigmaCompiler(ErgoAddressEncoder.MainnetNetworkPrefix)

  val depositEnv = Map(
    "Pk"      -> dummyPk,
    "PoolNFT" -> dummyDigest32,
    "DexFee"  -> dummyLong
  )

  val depositTree =
    sigmaUtils.updateVersionHeader(ErgoTree.fromProposition(sigma.compile(depositEnv, deposit).asSigmaProp))

  depositTree.constants.zipWithIndex.foreach { case (c, i) => println(s"{$i} -> $c") }
  println("[Deposit] ErgoTree:         " + ErgoTreeSerializer.default.serialize(depositTree))
  println("[Deposit] ErgoTreeTemplate: " + ErgoTreeTemplate.fromBytes(depositTree.template))

  val redeemTree =
    sigmaUtils.updateVersionHeader(ErgoTree.fromProposition(sigma.compile(depositEnv, redeem).asSigmaProp))

  println()
  redeemTree.constants.zipWithIndex.foreach { case (c, i) => println(s"{$i} -> $c") }
  println("[Redeem] ErgoTree:         " + ErgoTreeSerializer.default.serialize(redeemTree))
  println("[Redeem] ErgoTreeTemplate: " + ErgoTreeTemplate.fromBytes(redeemTree.template))

  val swapEnv = Map(
    "Pk"             -> dummyPk,
    "PoolNFT"        -> Array.fill(32)(1.toByte),
    "MinQuoteAmount" -> 89L,
    "QuoteId"        -> Array.fill(32)(2.toByte)
  )

  val swapTree =
    sigmaUtils.updateVersionHeader(ErgoTree.fromProposition(sigma.compile(swapEnv, swap).asSigmaProp))

  println()
  swapTree.constants.zipWithIndex.foreach { case (c, i) => println(s"{$i} -> $c") }
  println("[Swap] ErgoTree:         " + ErgoTreeSerializer.default.serialize(swapTree))
  println("[Swap] ErgoTreeTemplate: " + ErgoTreeTemplate.fromBytes(swapTree.template))

  val poolEnv = Map.empty[String, Any]

  val poolTree =
    sigmaUtils.updateVersionHeader(ErgoTree.fromProposition(sigma.compile(poolEnv, pool).asSigmaProp))

  println()
  poolTree.constants.zipWithIndex.foreach { case (c, i) => println(s"{$i} -> $c") }
  println("[Pool] ErgoTree:         " + ErgoTreeSerializer.default.serialize(poolTree))
  println("[Pool] ErgoTreeTemplate: " + ErgoTreeTemplate.fromBytes(poolTree.template))
  println("[Pool] ErgoTreeTemplateHash: " + ErgoTreeTemplate.fromBytes(Sha256.hash(poolTree.template)))
  println(
    "[Pool] Address: " + new ErgoAddressEncoder(ErgoAddressEncoder.MainnetNetworkPrefix).fromProposition(poolTree)
  )
}
