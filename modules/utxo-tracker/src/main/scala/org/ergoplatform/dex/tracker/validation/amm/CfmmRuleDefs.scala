package org.ergoplatform.dex.tracker.validation.amm

import cats.Applicative
import org.ergoplatform.dex.domain.NetworkContext
import org.ergoplatform.dex.domain.amm.{CfmmOperation, Deposit, Redeem, Swap}
import org.ergoplatform.dex.tracker.configs.Fees
import tofu.syntax.embed._
import tofu.syntax.monadic._

final class CfmmRuleDefs[F[_]: Applicative](constraints: Fees, network: NetworkContext) {

  type CfmmRule = CfmmOperation PartialFunction Boolean

  def rules: CfmmRules[F] = op => allRules.lift(op).getOrElse(true).pure

  private val allRules = sufficientValueDepositRedeem orElse sufficientValueSwap

  private val safeMinValue = network.params.safeMinValue

  private def sufficientValueDepositRedeem: CfmmRule = {
    case Deposit(params, _) => params.dexFee - constraints.minerFee >= safeMinValue
    case Redeem(params, _)  => params.dexFee - constraints.minerFee >= safeMinValue
  }

  private def sufficientValueSwap: CfmmRule = { case Swap(params, _) =>
    params.dexFeePerToken * params.minOutput.value - constraints.minerFee >= safeMinValue
  }
}
