package org.ergoplatform.dex.demo

import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.ergo.ErgoTreeTemplate
import org.ergoplatform.dex.protocol.{ErgoTreeSerializer, sigmaUtils}
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
    "Pk"                -> dummyPk,
    "PoolNFT"           -> dummyDigest32,
    "DexFee"            -> dummyLong
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
    "PoolScriptHash" -> Array.fill(32)(1.toByte),
    "DexFeePerToken" -> 99L,
    "MinQuoteAmount" -> 89L,
    "QuoteId"        -> Array.fill(32)(2.toByte),
    "FeeNum"         -> 998L
  )

  // 198f030f08cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d040004040e20000000000000000000000000000000000000000000000000000000000000000004060400050204d00f05cc0f0e200202020202020202020202020202020202020202020202020202020202020202040005b20105c601060203e6060203e6d810d6017300d602b2a4730100d603db63087202d604b27203730200d6058c720401d6067303d607b27203730400d6088c720701d609b2db6308a7730500d60a8c720901d60b7e8c72040206d60c998c7209027306d60d7e720c06d60e7e8c72070206d60f7307d6107e9c720c730806eb027201d1ededed93cbc272027309ec93720572069372087206ec937205720a937208720aaea5d9011163d802d613b2db63087211730a00d6148c721302edededed93c27211d07201938c7213017206927214730b92c1721199c1a79c7214730c959372057206909c9c720b720d730d9c7e7214069a9c720e7e720f067210909c9c720e720d730e9c7e7214069a9c720b7e720f067210
  // 19ed020e08cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d04000e200202020202020202020202020202020202020202020202020202020202020202040404060400050204d00f05cc0f040005b20105c601060203e6060203e6d810d6017300d602b2a4730100d6037302d604db63087202d605b27204730300d6068c720501d607b27204730400d6088c720701d609b2db6308a7730500d60a8c720901d60b7e8c72050206d60c998c7209027306d60d7e720c06d60e7e8c72070206d60f7307d6107e9c720c730806eb027201d1ededed93cbc272027203ec93720672039372087203ec937206720a937208720aaea5d9011163d802d613b2db63087211730900d6148c721302edededed93c27211d07201938c7213017203927214730a92c1721199c1a79c7214730b959372067203909c9c720b720d730c9c7e7214069a9c720e7e720f067210909c9c720e720d730d9c7e7214069a9c720b7e720f067210

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
  println("[Pool] Address: " + new ErgoAddressEncoder(ErgoAddressEncoder.MainnetNetworkPrefix).fromProposition(poolTree))
}
