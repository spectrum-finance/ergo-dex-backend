package org.ergoplatform.dex.executor.amm.generators

import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.protocol.{sigmaUtils, ErgoTreeSerializer}
import org.ergoplatform.dex.sources.n2tContracts
import scorex.util.encode.Base16
import sigmastate.Values.ErgoTree
import sigmastate.lang.Terms.ValueOps
import sigmastate.basics.DLogProtocol.DLogProverInput
import sigmastate.eval.{CompiletimeIRContext, IRContext}
import sigmastate.lang.SigmaCompiler

object ErgoTreeGenerator {

  implicit private val IR: IRContext = new CompiletimeIRContext()
  val sigma                          = SigmaCompiler(ErgoAddressEncoder.MainnetNetworkPrefix)

  val env = Map(
    "Pk"          -> DLogProverInput(BigInt(Long.MaxValue).bigInteger).publicImage,
    "PoolNFT"     -> Array.fill(32)(0: Byte),
    "QuoteId"     -> Array.fill(32)(1.toByte),
    "DexFee"      -> 999999L,
    "SelfX"       -> 888888L,
    "MaxMinerFee" -> 777777L,
    "MinerPropBytes" -> Base16
      .decode(
        "1005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a57304"
      )
      .get
  )

  val source = n2tContracts.swapSell

  val tree =
    sigmaUtils.updateVersionHeader(ErgoTree.fromProposition(sigma.compile(env, source).asSigmaProp))

  val serializedSwapTree = ErgoTreeSerializer.default.serialize(tree)
}
