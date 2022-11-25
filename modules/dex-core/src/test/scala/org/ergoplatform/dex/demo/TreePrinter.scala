package org.ergoplatform.dex.demo

import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.protocol.{sigmaUtils, ErgoTreeSerializer}
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
    "Pk"                -> DLogProverInput(BigInt(Long.MaxValue).bigInteger).publicImage,
    "PoolNFT"           -> Array.fill(32)(0: Byte),
    "QuoteId"           -> Array.fill(32)(1.toByte),
    "DexFee"            -> 999999L,
    "SelfX"             -> 888888L,
    "MaxMinerFee"       -> 777777L,
    "SpectrumId"        -> Array.fill(32)(2.toByte),
    "RedeemerPropBytes" -> Array.fill(32)(3.toByte),
    "RefundProp"        -> false,
    "MinerPropBytes" -> Base16
      .decode(
        "1005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a57304"
      )
      .get,
    "SelfXAmount" -> 999,
    "SpectrumIsY" -> false,
    "ExFee" -> 123
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
    "LqLock" -> lockContracts.lock
  )

  def printAll(cat: String, trees: List[(String, String)]): Unit = {
    println(cat)
    println("^" * 80)
    trees.foreach { case (sign, tree) => printTree(sign, tree) }
  }

//  printAll("N2T", N2T_Trees)
//  printAll("T2T", T2T_Trees)
//  printAll("DeFi_Prims", Primitives_Trees)

  printTree("depositV3", n2tContracts.swapBuyV3)

  val src =
    "1999030f0400040204020404040405feffffffffffffffff0105feffffffffffffffff01050004d00f040004000406050005000580dac409d819d601b2a5730000d602e4c6a70404d603db63087201d604db6308a7d605b27203730100d606b27204730200d607b27203730300d608b27204730400d6099973058c720602d60a999973068c7205027209d60bc17201d60cc1a7d60d99720b720cd60e91720d7307d60f8c720802d6107e720f06d6117e720d06d612998c720702720fd6137e720c06d6147308d6157e721206d6167e720a06d6177e720906d6189c72117217d6199c72157217d1ededededededed93c27201c2a793e4c672010404720293b27203730900b27204730a00938c7205018c720601938c7207018c72080193b17203730b9593720a730c95720e929c9c721072117e7202069c7ef07212069a9c72137e7214067e9c720d7e72020506929c9c721372157e7202069c7ef0720d069a9c72107e7214067e9c72127e7202050695ed720e917212730d907216a19d721872139d72197210ed9272189c721672139272199c7216721091720b730e"
  val tree = ErgoTreeSerializer.default.deserialize(SErgoTree.unsafeFromString(src))
//  tree.constants.zipWithIndex.foreach { case (c, i) => println(s"{$i} -> $c") }
}
