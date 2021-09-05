package org.ergoplatform.dex.demo

import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.protocol.{ErgoTreeSerializer, sigmaUtils}
import org.ergoplatform.dex.sources.{n2tContracts, t2tContracts}
import org.ergoplatform.ergo.{ErgoTreeTemplate, SErgoTree}
import sigmastate.Values.ErgoTree
import sigmastate.basics.DLogProtocol.DLogProverInput
import sigmastate.eval.{CompiletimeIRContext, IRContext}
import sigmastate.lang.SigmaCompiler
import sigmastate.lang.Terms.ValueOps

object TreePrinter extends App {

  implicit private val IR: IRContext = new CompiletimeIRContext()
  val sigma                          = SigmaCompiler(ErgoAddressEncoder.MainnetNetworkPrefix)

  val env = Map(
    "Pk"             -> DLogProverInput(BigInt(Long.MaxValue).bigInteger).publicImage,
    "PoolNFT"        -> Array.fill(32)(0: Byte),
    "QuoteId"        -> Array.fill(32)(1.toByte),
    "DexFee"         -> 999999L,
    "SelfX"          -> 888888L,
  )

  def parseTree(raw: String): Unit = {
    val tree = ErgoTreeSerializer.default.deserialize(SErgoTree.unsafeFromString(raw))

    println(s"Constants:")
    tree.constants.zipWithIndex.foreach { case (c, i) => println(s"{$i} -> $c") }
    println()
  }

  def printTree(signature: String, source: String): Unit = {
    val tree =
      sigmaUtils.updateVersionHeader(ErgoTree.fromProposition(sigma.compile(env, source).asSigmaProp))

    println(s"[$signature] Constants:")
    tree.constants.zipWithIndex.foreach { case (c, i) => println(s"{$i} -> $c") }
    println("* " * 40)
    println(s"[$signature] ErgoTree:         " + ErgoTreeSerializer.default.serialize(tree))
    println(s"[$signature] ErgoTreeTemplate: " + ErgoTreeTemplate.fromBytes(tree.template))
    println("-" * 80)
    println()
  }

  val T2T_Trees = List(
    "Deposit" -> t2tContracts.deposit,
    "Redeem"  -> t2tContracts.redeem,
    "Swap"    -> t2tContracts.swap,
    "Pool"    -> t2tContracts.pool
  )

  val N2T_Trees = List(
    "Deposit"  -> n2tContracts.deposit,
    "Redeem"   -> n2tContracts.redeem,
    "SwapSell" -> n2tContracts.swapSell,
    "SwapBuy"  -> n2tContracts.swapBuy,
    "Pool"     -> n2tContracts.pool
  )

  def printAll(cat: String, trees: List[(String, String)]): Unit = {
    println(cat)
    println("^" * 80)
    trees.foreach { case (sign, tree) => printTree(sign, tree) }
  }

  printAll("N2T", N2T_Trees)
}
