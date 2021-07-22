package org.ergoplatform.dex.tracker.validation.amm

import cats.Applicative
import org.ergoplatform.dex.configs.ExecutionConfig
import org.ergoplatform.dex.domain.NetworkContext
import org.ergoplatform.dex.domain.amm.{CFMMOperationRequest, Deposit, Redeem, Swap}
import tofu.syntax.embed._
import tofu.syntax.monadic._

import scala.{PartialFunction => ?=>}

final class CfmmRuleDefs[F[_]: Applicative](conf: ExecutionConfig) {

  type CFMMRule = CFMMOperationRequest ?=> Boolean

  def rules: CFMMRules[F] = op => allRules.lift(op).getOrElse(true).pure

  private val allRules = sufficientValueDepositRedeem orElse sufficientValueSwap

  private def sufficientValueDepositRedeem: CFMMRule = {
    case Deposit(_, params, _) => params.dexFee - conf.minerFee >= conf.minDexFee
    case Redeem(_, params, _)  => params.dexFee - conf.minerFee >= conf.minDexFee
  }

  private def sufficientValueSwap: CFMMRule = { case Swap(_, params, _) =>
    params.dexFeePerTokenNum * params.minOutput.value / params.dexFeePerTokenDenom - conf.minerFee >= conf.minDexFee
  }
}
