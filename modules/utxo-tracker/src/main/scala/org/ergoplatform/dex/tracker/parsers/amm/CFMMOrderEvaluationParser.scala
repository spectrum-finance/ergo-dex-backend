package org.ergoplatform.dex.tracker.parsers.amm

import cats.Applicative
import cats.syntax.option._
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.amm.CFMMPool
import org.ergoplatform.dex.domain.amm.CFMMVersionedOrder.{VersionedDeposit, VersionedRedeem, VersionedSwap}
import org.ergoplatform.dex.domain.amm.OrderEvaluation.{DepositEvaluation, RedeemEvaluation, SwapEvaluation}
import org.ergoplatform.ergo.domain.Output
import tofu.syntax.monadic._

trait CFMMOrderEvaluationParser[F[_]] {

  def parseSwapEval(output: Output, order: VersionedSwap): F[Option[SwapEvaluation]]

  def parseDepositEval(output: Output, pool: CFMMPool, order: VersionedDeposit): F[Option[DepositEvaluation]]

  def parseRedeemEval(output: Output, pool: CFMMPool, order: VersionedRedeem): F[Option[RedeemEvaluation]]
}

object CFMMOrderEvaluationParser {

  implicit def universalEvalParser[F[_]: Applicative]: CFMMOrderEvaluationParser[F] =
    new UniversalParser[F]

  final class UniversalParser[F[_]: Applicative] extends CFMMOrderEvaluationParser[F] {

    def parseSwapEval(output: Output, order: VersionedSwap): F[Option[SwapEvaluation]] =
      if (output.ergoTree == order.params.redeemer.ergoTree) {
        val outputAmount =
          if (order.params.minOutput.isNative) Some(AssetAmount.native(output.value))
          else output.assets.find(_.tokenId == order.params.minOutput.id).map(AssetAmount.fromBoxAsset)
        outputAmount.map(out => SwapEvaluation(out)).pure
      } else none[SwapEvaluation].pure

    def parseDepositEval(output: Output, pool: CFMMPool, order: VersionedDeposit): F[Option[DepositEvaluation]] =
      if (output.ergoTree == order.params.redeemer.ergoTree) {
        val outputAmountLP =
          output.assets.find(_.tokenId == pool.lp.id).map(AssetAmount.fromBoxAsset)
        outputAmountLP.map(out => DepositEvaluation(out)).pure
      } else none[DepositEvaluation].pure

    def parseRedeemEval(output: Output, pool: CFMMPool, order: VersionedRedeem): F[Option[RedeemEvaluation]] =
      if (output.ergoTree == order.params.redeemer.ergoTree) {
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
