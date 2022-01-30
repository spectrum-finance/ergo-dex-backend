package org.ergoplatform.dex.demo

import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.protocol.{ErgoTreeSerializer, sigmaUtils}
import org.ergoplatform.dex.sources.{lockContracts, n2tContracts, t2tContracts}
import org.ergoplatform.ergo.{ErgoTreeTemplate, SErgoTree}
import scorex.crypto.hash.Sha256
import scorex.util.encode.Base16
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
    "MaxMinerFee"    -> 777777L,
    "MinerPropBytes" -> Base16.decode("1005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a57304").get
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
    println(s"[$signature] ErgoTree:             " + ErgoTreeSerializer.default.serialize(tree))
    println(s"[$signature] ErgoTreeTemplate:     " + ErgoTreeTemplate.fromBytes(tree.template))
    println(s"[$signature] ErgoTreeTemplateHash: " + Base16.encode(Sha256.hash(tree.template)))
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

  val Primitives_Trees = List(
    "LqLock"  -> lockContracts.lock,
  )

  def printAll(cat: String, trees: List[(String, String)]): Unit = {
    println(cat)
    println("^" * 80)
    trees.foreach { case (sign, tree) => printTree(sign, tree) }
  }

  printAll("N2T", N2T_Trees)
  printAll("T2T", T2T_Trees)
  printAll("DeFi_Prims", Primitives_Trees)

  //printTree("deposit", n2tContracts.deposit)
//  val src = "195e03040004000400d802d601b2a5730000d602e4c6a70404ea02e4c6a70508d19593c27201c2a7d802d603b2db63087201730100d604b2db6308a7730200eded92e4c6720104047202938c7203018c720401928c7203028c7204028f7202a3"
//  val tree = ErgoTreeSerializer.default.deserialize(SErgoTree.unsafeFromString(src))
//  tree.constants.zipWithIndex.foreach { case (c, i) => println(s"{$i} -> $c") }
}
