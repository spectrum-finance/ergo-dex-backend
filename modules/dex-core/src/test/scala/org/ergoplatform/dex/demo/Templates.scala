package org.ergoplatform.dex.demo

import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.ErgoTreeTemplate
import org.ergoplatform.dex.protocol.{sigmaUtils, ErgoTreeSerializer}
import sigmastate.Values.ErgoTree
import sigmastate.basics.DLogProtocol.DLogProverInput
import sigmastate.eval.{CompiletimeIRContext, IRContext}
import sigmastate.lang.SigmaCompiler
import sigmastate.lang.Terms.ValueOps

object Templates extends App {

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

  def deposit =
    """
      |{
      |    val selfX = SELF.tokens(0)
      |    val selfY = SELF.tokens(1)
      |
      |    val poolIn = INPUTS(0)
      |
      |    val validPoolIn = poolIn.tokens(0) == (PoolNFT, 1L)
      |
      |    val poolLP    = poolIn.tokens(1)
      |    val reservesX = poolIn.tokens(2)
      |    val reservesY = poolIn.tokens(2)
      |
      |    val supplyLP = InitiallyLockedLP - poolLP._2
      |
      |    val minimalReward = min(
      |        selfX._2.toBigInt * supplyLP / reservesX._2,
      |        selfY._2.toBigInt * supplyLP / reservesY._2
      |    )
      |
      |    val rewardOut = OUTPUTS(1)
      |    val rewardLP  = rewardOut.tokens(0)
      |
      |    val validRewardOut =
      |        rewardOut.propositionBytes == Pk.propBytes &&
      |        rewardOut.value >= SELF.value - DexFee &&
      |        rewardLP._1 == poolLP._1 &&
      |        rewardLP._2 >= minimalReward
      |
      |    sigmaProp(Pk || (validPoolIn && validRewardOut))
      |}
      |""".stripMargin

  def redeem =
    """
      |{
      |    val selfLP = SELF.tokens(0)
      |
      |    val poolIn = INPUTS(0)
      |
      |    val validPoolIn = poolIn.tokens(0) == (PoolNFT, 1L)
      |
      |    val poolLP    = poolIn.tokens(1)
      |    val reservesX = poolIn.tokens(2)
      |    val reservesY = poolIn.tokens(2)
      |
      |    val supplyLP = InitiallyLockedLP - poolLP._2
      |
      |    val minReturnX = selfLP._2.toBigInt * reservesX._2 / supplyLP
      |    val minReturnY = selfLP._2.toBigInt * reservesY._2 / supplyLP
      |
      |    val returnOut = OUTPUTS(1)
      |
      |    val returnX = returnOut.tokens(0)
      |    val returnY = returnOut.tokens(1)
      |
      |    val validReturnOut =
      |        returnOut.propositionBytes == Pk.propBytes &&
      |        returnOut.value >= SELF.value - DexFee &&
      |        returnX._1 == reservesX._1 &&
      |        returnY._1 == reservesY._1 &&
      |        returnX._2 >= minReturnX &&
      |        returnY._2 >= minReturnY
      |
      |    sigmaProp(Pk || (validPoolIn && validReturnOut))
      |}
      |""".stripMargin

  def swap =
    """
      |{
      |    val FeeDenom = 1000
      |
      |    val base       = SELF.tokens(0)
      |    val baseId     = base._1
      |    val baseAmount = base._2
      |
      |    val poolInput  = INPUTS(0)
      |    val poolAssetX = poolInput.tokens(2)
      |    val poolAssetY = poolInput.tokens(3)
      |
      |    val validPoolInput =
      |        blake2b256(poolInput.propositionBytes) == PoolScriptHash &&
      |        (poolAssetX._1 == QuoteId || poolAssetY._1 == QuoteId) &&
      |        (poolAssetX._1 == baseId  || poolAssetY._1 == baseId)
      |
      |    val validTrade =
      |        OUTPUTS.exists { (box: Box) =>
      |            val quoteAsset   = box.tokens(0)
      |            val quoteAmount  = quoteAsset._2
      |            val fairDexFee   = box.value >= SELF.value - quoteAmount * DexFeePerToken
      |            val relaxedInput = baseAmount - 1
      |            val fairPrice    =
      |                if (poolAssetX._1 == QuoteId)
      |                    poolAssetX._2.toBigInt * relaxedInput * FeeNum <= quoteAmount * (poolAssetY._2.toBigInt * FeeDenom + relaxedInput * FeeNum)
      |                else
      |                    poolAssetY._2.toBigInt * relaxedInput * FeeNum <= quoteAmount * (poolAssetX._2.toBigInt * FeeDenom + relaxedInput * FeeNum)
      |
      |            box.propositionBytes == Pk.propBytes &&
      |            quoteAsset._1 == QuoteId &&
      |            quoteAsset._2 >= MinQuoteAmount &&
      |            fairDexFee &&
      |            fairPrice
      |        }
      |
      |    sigmaProp(Pk || (validPoolInput && validTrade))
      |}
      |""".stripMargin
}
