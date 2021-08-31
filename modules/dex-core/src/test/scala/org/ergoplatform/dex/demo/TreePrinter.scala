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

  parseTree("19ac020f08cd02c3f56e66191a903758f53a4b90d07cef80f93e7a4f17d106098ad0caf189722a040004040406040204000404040005feffffffffffffffff01040204000e20bee300e9c81e48d7ab5fc29294c7bbb536cf9dcd9c91ee3be9898faec91b11b60580dac40905fe887a0100d802d6017300d602b2a4730100eb027201d195ed93b1a4730293b1db630872027303d808d603db63087202d604b2a5730400d605c1a7d606c17204d607b2db63087204730500d608b27203730600d6097e8cb2db6308a77307000206d60a7e9973088cb272037309000206ededededed938cb27203730a0001730b93c27204d07201927206997205730c938c7207018c720801927e9a9972067205730d069d9c72097ec1720206720a927e8c720702069d9c72097e8c72080206720a730e")

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
