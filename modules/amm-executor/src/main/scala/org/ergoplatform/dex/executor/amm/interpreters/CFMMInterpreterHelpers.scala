package org.ergoplatform.dex.executor.amm.interpreters

import org.ergoplatform.ErgoBox.{NonMandatoryRegisterId, R4}
import org.ergoplatform.dex.configs.MonetaryConfig
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.amm.{CFMMPool, Swap}
import org.ergoplatform.dex.executor.amm.config.ExchangeConfig
import org.ergoplatform.dex.executor.amm.domain.errors.{ExecutionFailed, PriceTooHigh, PriceTooLow}
import org.ergoplatform.ergo.TokenId
import org.ergoplatform.ergo.syntax._
import org.ergoplatform.{ErgoAddressEncoder, ErgoBox, ErgoScriptPredef, Pay2SAddress}
import sigmastate.Values.IntConstant
import sigmastate.eval._
import sigmastate.{SInt, Values}
import special.collection.Coll

final class CFMMInterpreterHelpers(
  exchange: ExchangeConfig,
  execution: MonetaryConfig
)(implicit encoder: ErgoAddressEncoder) {

  val minerFeeProp: Values.ErgoTree = Pay2SAddress(ErgoScriptPredef.feeProposition()).script
  val dexFeeProp: Values.ErgoTree   = exchange.rewardAddress.toErgoTree

  def swapParams(swap: Swap, pool: CFMMPool): Either[ExecutionFailed, (AssetAmount, AssetAmount, Long)] = {
    val input  = swap.params.input
    val output = pool.outputAmount(input)
    val dexFee = (BigInt(output.value) * swap.params.dexFeePerTokenNum /
      swap.params.dexFeePerTokenDenom - execution.minerFee).toLong
    val maxDexFee = swap.box.value - execution.minerFee - execution.minBoxValue
    if (output < swap.params.minOutput) Left(PriceTooHigh(swap.poolId, swap.params.minOutput, output))
    else if (dexFee > maxDexFee) Left(PriceTooLow(swap.poolId, maxDexFee, dexFee))
    else Right((input, output, dexFee))
  }

  def mkTokens(tokens: (TokenId, Long)*): Coll[(ErgoBox.TokenId, Long)] =
    Colls.fromItems(tokens.map { case (k, v) => k.toErgo -> v }: _*)

  def mkPoolRegs(pool: CFMMPool): Map[NonMandatoryRegisterId, Values.Constant[SInt.type]] =
    scala.Predef.Map(
      (R4: NonMandatoryRegisterId) -> IntConstant(pool.feeNum)
    )
}
