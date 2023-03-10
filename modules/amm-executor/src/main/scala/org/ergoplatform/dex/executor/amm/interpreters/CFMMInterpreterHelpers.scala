package org.ergoplatform.dex.executor.amm.interpreters

import org.ergoplatform.ErgoBox.{NonMandatoryRegisterId, R4}
import org.ergoplatform._
import org.ergoplatform.dex.configs.MonetaryConfig
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.amm.CFMMOrder.{SwapErg, SwapMultiAddress, SwapP2Pk, SwapTokenFee}
import org.ergoplatform.dex.domain.amm.CFMMPool
import org.ergoplatform.dex.executor.amm.config.ExchangeConfig
import org.ergoplatform.dex.executor.amm.domain.errors.{ExecutionFailed, NegativeDexFee, PriceTooHigh, PriceTooLow}
import org.ergoplatform.ergo.syntax._
import org.ergoplatform.ergo.{PrivKeyGenerator, TokenId}
import sigmastate.Values.IntConstant
import sigmastate.basics.DLogProtocol.DLogProverInput
import sigmastate.eval._
import sigmastate.{SInt, Values}
import special.collection.Coll

final class CFMMInterpreterHelpers(
  val exchange: ExchangeConfig,
  monetary: MonetaryConfig
)(implicit encoder: ErgoAddressEncoder) {

  val minerFeeProp: Values.ErgoTree = Pay2SAddress(ErgoScriptPredef.feeProposition()).script

  val sk: DLogProverInput = PrivKeyGenerator.make(exchange.mnemonic)._1

  def swapParamsErgFee(swap: SwapErg, pool: CFMMPool): Either[ExecutionFailed, (AssetAmount, AssetAmount, Long)] = {
    val params = swap match {
      case s: SwapP2Pk         => s.params
      case s: SwapMultiAddress => s.params
    }
    val baseAmount  = params.baseAmount
    val quoteAmount = pool.outputAmount(baseAmount)
    val feeFactor   = BigDecimal(params.dexFeePerTokenNum) / params.dexFeePerTokenDenom
    val dexFee      = (quoteAmount.value * feeFactor - monetary.minerFee).toLong
    val maxDexFee   = swap.box.value - monetary.minerFee - monetary.minBoxValue
    if (quoteAmount < params.minQuoteAmount) Left(PriceTooHigh(swap.poolId, params.minQuoteAmount, quoteAmount))
    else if (dexFee > maxDexFee) Left(PriceTooLow(swap.poolId, maxDexFee, dexFee, quoteAmount.value))
    else if (dexFee < 0) Left(NegativeDexFee(swap.poolId, swap.id, dexFee))
    else Right((baseAmount, quoteAmount, dexFee))
  }

  def swapParamsTokenFee(
    swap: SwapTokenFee,
    pool: CFMMPool
  ): Either[ExecutionFailed, (AssetAmount, AssetAmount, Long)] = {
    val baseAmount: AssetAmount  = swap.params.baseAmount
    val quoteAmount: AssetAmount = pool.outputAmount(baseAmount)
    val feeFactor: BigDecimal    = BigDecimal(swap.params.dexFeePerTokenNum) / swap.params.dexFeePerTokenDenom
    val dexFee: Long             = (quoteAmount.value * feeFactor).toLong
    val maxDexFee: Long          = swap.reservedExFee
    if (quoteAmount < swap.params.minQuoteAmount)
      Left(PriceTooHigh(swap.poolId, swap.params.minQuoteAmount, quoteAmount))
    else if (dexFee > maxDexFee) Left(PriceTooLow(swap.poolId, maxDexFee, dexFee, quoteAmount.value))
    else if (dexFee < 0) Left(NegativeDexFee(swap.poolId, swap.id, dexFee))
    else Right((baseAmount, quoteAmount, dexFee))
  }

  def mkTokens(tokens: (TokenId, Long)*): Coll[(ErgoBox.TokenId, Long)] =
    Colls.fromItems(tokens.map { case (k, v) => k.toErgo -> v }: _*)

  def mkPoolRegs(pool: CFMMPool): Map[NonMandatoryRegisterId, Values.Constant[SInt.type]] =
    scala.Predef.Map(
      (R4: NonMandatoryRegisterId) -> IntConstant(pool.feeNum)
    )

  def mkPoolTokens(pool: CFMMPool, amountLP: Long, amountY: Long): Coll[(ErgoBox.TokenId, Long)] =
    mkTokens(
      pool.poolId.value -> 1L,
      pool.lp.id        -> amountLP,
      pool.y.id         -> amountY
    )

  def mkPoolTokens(pool: CFMMPool, amountLP: Long, amountX: Long, amountY: Long): Coll[(ErgoBox.TokenId, Long)] =
    mkTokens(
      pool.poolId.value -> 1L,
      pool.lp.id        -> amountLP,
      pool.x.id         -> amountX,
      pool.y.id         -> amountY
    )
}
