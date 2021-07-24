package org.ergoplatform.dex.tracker.validation.amm

import cats.Applicative
import org.ergoplatform.dex.configs.ExecutionConfig
import org.ergoplatform.dex.domain.amm.{CFMMOperationRequest, Deposit, Redeem, Swap}
import tofu.syntax.embed._
import tofu.syntax.monadic._

import scala.{PartialFunction => ?=>}

final class CfmmRuleDefs[F[_]: Applicative](conf: ExecutionConfig) {

  type CFMMRule = CFMMOperationRequest ?=> Option[RuleViolation]

  def rules: CFMMRules[F] = op => allRules.lift(op).flatten.pure

  private val allRules = sufficientValueDepositRedeem orElse sufficientValueSwap

  private def sufficientValueDepositRedeem: CFMMRule = {
    case Deposit(_, params, _) => checkFee(params.dexFee)
    case Redeem(_, params, _)  => checkFee(params.dexFee)
  }

  private def sufficientValueSwap: CFMMRule = { case Swap(_, params, box) =>
    val minDexFee     = BigInt(params.dexFeePerTokenNum) * params.minOutput.value / params.dexFeePerTokenDenom
    val sufficientFee = checkFee(minDexFee)
    val maxDexFee     = box.value - conf.minerFee - conf.minBoxValue
    val sufficientValue =
      if (maxDexFee >= minDexFee) None
      else Some(s"Actual fee '$maxDexFee' is less than declared minimum '$minDexFee'")
    sufficientFee orElse sufficientValue
  }

  private def checkFee(givenFee: BigInt): Option[RuleViolation] =
    if (givenFee >= conf.minDexFee) None
    else Some(s"Declared fee '$givenFee' is less than required minimum '${conf.minDexFee}'")
}
