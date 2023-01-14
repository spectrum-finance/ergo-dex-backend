package org.ergoplatform.dex.index.processes

import cats.{Applicative, Monad}
import cats.data.NonEmptyList
import cats.syntax.option.none
import org.ergoplatform.dex.domain.amm.CFMMVersionedOrder.{AnyDeposit, AnyRedeem, AnySwap}
import org.ergoplatform.dex.domain.amm.{CFMMVersionedOrder, EvaluatedCFMMOrder}
import org.ergoplatform.dex.domain.amm.OrderEvaluation.{DepositEvaluation, RedeemEvaluation, SwapEvaluation}
import org.ergoplatform.dex.index.db.Extract.syntax.ExtractOps
import org.ergoplatform.dex.index.db.models.{DBDeposit, DBOrderExecutorFee, DBRedeem, DBSwap}
import org.ergoplatform.dex.index.repositories.{MonoRepo, RepoBundle}
import org.ergoplatform.graphite.Metrics
import tofu.syntax.monadic._

trait AnyOrdersHandler[F[_]] {
  def handle(anyOrders: List[EvaluatedCFMMOrder.Any]): F[Int]
}

object AnyOrdersHandler {

  def makeOrdersHandlers[F[_]: Monad](implicit
    repos: RepoBundle[F],
    metrics: Metrics[F]
  ): List[AnyOrdersHandler[F]] =
    new SwapHandler[F](repos.swaps) :: new RedeemHandler[F](repos.redeems) :: new DepositHandler[F](
      repos.deposits
    ) :: new OrderExecutorFeeHandler[F](repos.orderExecutorFee) :: Nil

  final private class OrderExecutorFeeHandler[F[_]: Monad](offChain: MonoRepo[DBOrderExecutorFee, F])(implicit
    metrics: Metrics[F]
  ) extends AnyOrdersHandler[F] {

    def handle(anyOrders: List[EvaluatedCFMMOrder.Any]): F[Int] =
      anyOrders
        .flatMap(_.orderExecutorFee)
        .map(_.extract[DBOrderExecutorFee]) match {
        case Nil => 0.pure
        case list @ head :: tl =>
          metrics.sendCount("order.executor.fee", list.length) >> offChain.insert(NonEmptyList(head, tl))
      }
  }

  final class SwapHandler[F[_]: Monad](repo: MonoRepo[DBSwap, F])(implicit metrics: Metrics[F])
    extends AnyOrdersHandler[F] {

    def handle(anyOrders: List[EvaluatedCFMMOrder.Any]): F[Int] =
      NonEmptyList.fromList(anyOrders.collect {
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.SwapMultiAddress, Some(ev: SwapEvaluation), p, r) =>
          EvaluatedCFMMOrder(o: AnySwap, Some(ev), p, r).extract[DBSwap]
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.SwapP2Pk, Some(ev: SwapEvaluation), p, r) =>
          EvaluatedCFMMOrder(o: AnySwap, Some(ev), p, r).extract[DBSwap]
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.SwapV0, Some(ev: SwapEvaluation), p, r) =>
          EvaluatedCFMMOrder(o: AnySwap, Some(ev), p, r).extract[DBSwap]
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.SwapV0, _, p, r) =>
          EvaluatedCFMMOrder(o: AnySwap, none[SwapEvaluation], p, r).extract[DBSwap]
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.SwapP2Pk, _, p, r) =>
          EvaluatedCFMMOrder(o: AnySwap, none[SwapEvaluation], p, r).extract[DBSwap]
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.SwapMultiAddress, _, p, r) =>
          EvaluatedCFMMOrder(o: AnySwap, none[SwapEvaluation], p, r).extract[DBSwap]
      }) match {
        case Some(nel) => metrics.sendCount("order.swap", nel.length) >> repo.insert(nel)
        case None      => 0.pure[F]
      }
  }

  final class RedeemHandler[F[_]: Monad](repo: MonoRepo[DBRedeem, F])(implicit metrics: Metrics[F])
    extends AnyOrdersHandler[F] {

    def handle(anyOrders: List[EvaluatedCFMMOrder.Any]): F[Int] =
      NonEmptyList.fromList(anyOrders.collect {
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.RedeemV1, Some(ev: RedeemEvaluation), p, r) =>
          EvaluatedCFMMOrder(o: AnyRedeem, Some(ev), p, r).extract[DBRedeem]
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.RedeemV0, Some(ev: RedeemEvaluation), p, r) =>
          EvaluatedCFMMOrder(o: AnyRedeem, Some(ev), p, r).extract[DBRedeem]
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.RedeemV1, _, p, r) =>
          EvaluatedCFMMOrder(o: AnyRedeem, none[RedeemEvaluation], p, r).extract[DBRedeem]
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.RedeemV0, _, p, r) =>
          EvaluatedCFMMOrder(o: AnyRedeem, none[RedeemEvaluation], p, r).extract[DBRedeem]
      }) match {
        case Some(nel) => metrics.sendCount("order.redeem", nel.length) >> repo.insert(nel)
        case None      => 0.pure[F]
      }
  }

  final class DepositHandler[F[_]: Monad](repo: MonoRepo[DBDeposit, F])(implicit metrics: Metrics[F])
    extends AnyOrdersHandler[F] {

    def handle(anyOrders: List[EvaluatedCFMMOrder.Any]): F[Int] =
      NonEmptyList.fromList(anyOrders.collect {
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.DepositV0, Some(ev: DepositEvaluation), p, r) =>
          EvaluatedCFMMOrder(o: AnyDeposit, Some(ev), p, r).extract[DBDeposit]
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.DepositV1, Some(ev: DepositEvaluation), p, r) =>
          EvaluatedCFMMOrder(o: AnyDeposit, Some(ev), p, r).extract[DBDeposit]
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.DepositV2, Some(ev: DepositEvaluation), p, r) =>
          EvaluatedCFMMOrder(o: AnyDeposit, Some(ev), p, r).extract[DBDeposit]
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.DepositV0, _, p, r) =>
          EvaluatedCFMMOrder(o: AnyDeposit, none[DepositEvaluation], p, r).extract[DBDeposit]
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.DepositV1, _, p, r) =>
          EvaluatedCFMMOrder(o: AnyDeposit, none[DepositEvaluation], p, r).extract[DBDeposit]
        case EvaluatedCFMMOrder(o: CFMMVersionedOrder.DepositV2, _, p, r) =>
          EvaluatedCFMMOrder(o: AnyDeposit, none[DepositEvaluation], p, r).extract[DBDeposit]
      }) match {
        case Some(nel) => metrics.sendCount("order.deposit", nel.length) >> repo.insert(nel)
        case None      => 0.pure[F]
      }
  }

}
