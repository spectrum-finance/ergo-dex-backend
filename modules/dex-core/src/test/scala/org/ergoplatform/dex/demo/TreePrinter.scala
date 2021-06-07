package org.ergoplatform.dex.demo

import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.ErgoTreeTemplate
import org.ergoplatform.dex.protocol.{sigmaUtils, ErgoTreeSerializer}
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
    "Pk"                -> dummyPk,
    "InitiallyLockedLP" -> 1000000000000000000L,
    "PoolNFT"           -> dummyDigest32,
    "DexFee"            -> dummyLong
  )

  val depositTree =
    sigmaUtils.updateVersionHeader(ErgoTree.fromProposition(sigma.compile(depositEnv, deposit).asSigmaProp))

  //depositTree.constants.zipWithIndex.foreach { case (c, i) => println(s"{$i} -> $c") }
  println("[Deposit] ErgoTree:         " + ErgoTreeSerializer.default.serialize(depositTree))
  println("[Deposit] ErgoTreeTemplate: " + ErgoTreeTemplate.fromBytes(depositTree.template))

  val redeemTree =
    sigmaUtils.updateVersionHeader(ErgoTree.fromProposition(sigma.compile(depositEnv, redeem).asSigmaProp))

  println()
  println("[Redeem] ErgoTree:         " + ErgoTreeSerializer.default.serialize(redeemTree))
  println("[Redeem] ErgoTreeTemplate: " + ErgoTreeTemplate.fromBytes(redeemTree.template))

  val swapEnv = Map(
    "Pk"             -> dummyPk,
    "PoolScriptHash" -> dummyDigest32,
    "DexFeePerToken" -> dummyLong,
    "MinQuoteAmount" -> dummyLong,
    "QuoteId"        -> dummyDigest32,
    "FeeNum"         -> dummyLong
  )

  val swapTree =
    sigmaUtils.updateVersionHeader(ErgoTree.fromProposition(sigma.compile(swapEnv, swap).asSigmaProp))

  println()
  println("[Swap] ErgoTree:         " + ErgoTreeSerializer.default.serialize(swapTree))
  println("[Swap] ErgoTreeTemplate: " + ErgoTreeTemplate.fromBytes(swapTree.template))

  val poolEnv = Map.empty[String, Any]

  val poolTree =
    sigmaUtils.updateVersionHeader(ErgoTree.fromProposition(sigma.compile(poolEnv, pool).asSigmaProp))

  println()
  poolTree.constants.zipWithIndex.foreach { case (c, i) => println(s"{$i} -> $c") }
  println("[Pool] ErgoTree:         " + ErgoTreeSerializer.default.serialize(poolTree))
  println("[Pool] ErgoTreeTemplate: " + ErgoTreeTemplate.fromBytes(poolTree.template))
}
