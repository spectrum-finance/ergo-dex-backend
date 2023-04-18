package org.ergoplatform.dex.tracker.validation.amm

import cats.Applicative
import org.ergoplatform.dex.configs.MonetaryConfig
import org.ergoplatform.dex.domain.amm.CFMMOrder
import org.ergoplatform.dex.domain.amm.CFMMOrder._
import org.ergoplatform.ergo.TokenId
import tofu.syntax.monadic._

import scala.{PartialFunction => ?=>}

final class CfmmRuleDefs[F[_]: Applicative](conf: MonetaryConfig, spf: TokenId) {

  type CFMMRule = CFMMOrder.AnyOrder ?=> Option[RuleViolation]

  def rules: CFMMRules[F] = op => allRules.lift(op).flatten.pure

  private val allRules = sufficientValueDepositRedeem orElse sufficientValueSwap

  private def sufficientValueDepositRedeem: CFMMRule = {
    case deposit: DepositErgFee   => checkFee(deposit.params.dexFee, false)
    case deposit: DepositTokenFee => checkFee(deposit.params.dexFee, true)
    case redeem: RedeemErgFee     => checkFee(redeem.params.dexFee, false)
    case redeem: RedeemTokenFee   => checkFee(redeem.params.dexFee, true)
  }

  private def sufficientValueSwap: CFMMRule = { order =>
    (order match {
      case SwapP2Pk(_, maxMinerFee, _, params, box)         => Some((params, false, maxMinerFee, box, 0L))
      case SwapMultiAddress(_, maxMinerFee, _, params, box) => Some((params, false, maxMinerFee, box, 0L))
      case SwapTokenFee(_, maxMinerFee, _, params, box, reservedExFee) =>
        Some((params, true, maxMinerFee, box, reservedExFee))
      case _ => None
    }) match {
      case Some((params, isTokenFee, maxMinerFee, box, reservedExFee)) =>
        val feeFactor    = BigDecimal(params.dexFeePerTokenNum) / params.dexFeePerTokenDenom
        val minDexFee    = params.minQuoteAmount.value * feeFactor
        val nativeInput  = if (params.baseAmount.isNative) params.baseAmount.value else 0L
        val maxTokenFee  = reservedExFee
        val minerFee     = conf.minerFee min maxMinerFee
        val maxDexFeeErg = box.value - conf.minBoxValue - nativeInput
        val insufficientValue = {
          if (isTokenFee && maxTokenFee >= minDexFee) None
          else if (maxDexFeeErg >= minDexFee) None
          else Some(s"Actual fee '$maxDexFeeErg' or '$maxTokenFee' is less than declared minimum '$minDexFee'")
        }
        val maxDexFeeNetErg = maxDexFeeErg - minerFee
        val insufficientFee = if (isTokenFee) checkFee(maxTokenFee, true) else checkFee(maxDexFeeNetErg, false)
        insufficientFee orElse insufficientValue
      case None => None
    }

  }

  private def checkFee(givenFee: BigInt, tokenFee: Boolean): Option[RuleViolation] =
    if (tokenFee && givenFee >= conf.minDexTokenFee) None
    else if (givenFee >= conf.minDexFee) None
    else Some(s"Declared fee '$givenFee' is less than configured minimum '${conf.minDexFee}'")
}
