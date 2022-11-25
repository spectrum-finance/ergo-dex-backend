package org.ergoplatform.dex.tracker.parsers.amm

import cats.Applicative
import cats.syntax.option._
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.amm.{CFMMPool, CFMMVersionedOrder}
import org.ergoplatform.dex.domain.amm.OrderEvaluation.{DepositEvaluation, RedeemEvaluation, SwapEvaluation}
import org.ergoplatform.ergo.PubKey
import org.ergoplatform.ergo.domain.Output
import tofu.syntax.monadic._

trait CFMMOrderEvaluationParser[F[_]] {

  def parseSwapEval(output: Output, order: CFMMVersionedOrder.AnySwap): F[Option[SwapEvaluation]]

  def parseDepositEval(
    output: Output,
    pool: CFMMPool,
    order: CFMMVersionedOrder.AnyDeposit
  ): F[Option[DepositEvaluation]]

  def parseRedeemEval(output: Output, pool: CFMMPool, order: CFMMVersionedOrder.AnyRedeem): F[Option[RedeemEvaluation]]
}

object CFMMOrderEvaluationParser {

  implicit def universalEvalParser[F[_]: Applicative]: CFMMOrderEvaluationParser[F] =
    new UniversalParser[F]

  final class UniversalParser[F[_]: Applicative] extends CFMMOrderEvaluationParser[F] {

    def parseSwapEval(output: Output, order: CFMMVersionedOrder.AnySwap): F[Option[SwapEvaluation]] = {
      val (redeemer, minOutput) = order match {
        case swap: CFMMVersionedOrder.SwapP2Pk => (swap.params.redeemer.ergoTree, swap.params.minQuoteAmount)
        case swap: CFMMVersionedOrder.SwapMultiAddress =>
          (swap.params.redeemer, swap.params.minQuoteAmount)
        case swap: CFMMVersionedOrder.SwapV0 => (swap.params.redeemer.ergoTree, swap.params.minQuoteAmount)
      }
      if (output.ergoTree == redeemer) {
        val outputAmount =
          if (minOutput.isNative) Some(AssetAmount.native(output.value))
          else output.assets.find(_.tokenId == minOutput.id).map(AssetAmount.fromBoxAsset)
        outputAmount.map(out => SwapEvaluation(out)).pure
      } else none[SwapEvaluation].pure
    }

    def parseDepositEval(
      output: Output,
      pool: CFMMPool,
      order: CFMMVersionedOrder.AnyDeposit
    ): F[Option[DepositEvaluation]] = {
      val config = order match {
        case deposit: CFMMVersionedOrder.DepositV2 => deposit.params
        case deposit: CFMMVersionedOrder.DepositV1 => deposit.params
        case deposit: CFMMVersionedOrder.DepositV0 => deposit.params
      }
      if (output.ergoTree == config.redeemer.ergoTree) {
        val outputAmountLP =
          output.assets.find(_.tokenId == pool.lp.id).map(AssetAmount.fromBoxAsset)
        outputAmountLP.map(out => DepositEvaluation(out)).pure
      } else none[DepositEvaluation].pure
    }

    def parseRedeemEval(
      output: Output,
      pool: CFMMPool,
      order: CFMMVersionedOrder.AnyRedeem
    ): F[Option[RedeemEvaluation]] = {
      val config = order match {
        case redeem: CFMMVersionedOrder.RedeemV1 => redeem.params
        case redeem: CFMMVersionedOrder.RedeemV0 => redeem.params
      }
      if (output.ergoTree == config.redeemer.ergoTree) {
        val outputAmountX =
          if (pool.x.isNative) Some(AssetAmount.native(output.value))
          else output.assets.find(_.tokenId == pool.x.id).map(AssetAmount.fromBoxAsset)
        val outputAmountY =
          if (pool.y.isNative) Some(AssetAmount.native(output.value))
          else output.assets.find(_.tokenId == pool.y.id).map(AssetAmount.fromBoxAsset)
        (for {
          outX <- outputAmountX
          outY <- outputAmountY
        } yield RedeemEvaluation(outX, outY)).pure
      } else none[RedeemEvaluation].pure
    }
  }
}
