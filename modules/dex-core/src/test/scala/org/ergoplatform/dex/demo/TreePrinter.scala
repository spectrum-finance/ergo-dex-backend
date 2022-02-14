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
  val src = "19b6031508cd03479af981aac1aa68bf10cc7d934f42193b3b796055cd9ef581ab377395496bdb0400040404060402058080a0f6f4acdbe01b05badc82d892fab1de1b040004000e203d4fdb931917647f4755ada29d13247686df84bd8aea060d22584081bd11ba6905c6c0eef1aa0104c60f060101040404d00f04c60f0e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a57304050005000580ade2040100d802d6017300d602b2a4730100eb027201d195ed93b1a4730293b1db630872027303d804d603db63087202d604b2a5730400d6059d9c7e99c17204c1a7067e7305067e730606d6068cb2db6308a773070002edededed938cb2720373080001730993c27204d072019272057e730a06909c9c7ec17202067e7206067e730b069c9a7205730c9a9c7e8cb27203730d0002067e730e067e9c72067e730f050690b0ada5d90107639593c272077310c1720773117312d90107599a8c7207018c72070273137314"
  val tree = ErgoTreeSerializer.default.deserialize(SErgoTree.unsafeFromString(src))
  tree.constants.zipWithIndex.foreach { case (c, i) => println(s"{$i} -> $c") }
}
