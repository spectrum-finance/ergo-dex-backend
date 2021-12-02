package org.ergoplatform.dex.tracker.parsers.amm

import org.ergoplatform.dex.domain.amm.OrderEvaluation.{DepositEvaluation, RedeemEvaluation, SwapEvaluation}
import org.ergoplatform.dex.domain.amm.{Deposit, Redeem, Swap}
import org.ergoplatform.ergo.models.Output

trait CFMMOrderEvaluationParser[F[_]] {

  def parseSwapEval(output: Output, order: Swap): F[Option[SwapEvaluation]]

  def parseDepositEval(output: Output, order: Deposit): F[Option[DepositEvaluation]]

  def parseRedeemEval(output: Output, order: Redeem): F[Option[RedeemEvaluation]]
}

object CFMMOrderEvaluationParser {

  implicit def universalEvalParser[F[_]]: CFMMOrderEvaluationParser[F] = ???
}
