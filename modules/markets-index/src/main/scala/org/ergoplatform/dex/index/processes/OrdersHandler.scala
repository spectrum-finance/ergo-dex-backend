package org.ergoplatform.dex.index.processes

import cats.data.NonEmptyList
import cats.{Applicative, Monad}
import org.ergoplatform.dex.domain.amm.OrderEvaluation.SwapEvaluation
import org.ergoplatform.dex.domain.amm.{CFMMOrder, EvaluatedCFMMOrder, Swap}
import org.ergoplatform.dex.index.db.models.{DBModel, DBSwap, DBSwapV1}
import org.ergoplatform.dex.index.db.Extract.syntax.ExtractOps
import tofu.syntax.foption._
import tofu.syntax.monadic._
import cats.syntax.traverse._
import org.ergoplatform.dex.index.repositories.RepoBundle

trait OrdersHandler[B] {
  def handle(anyOrders: List[EvaluatedCFMMOrder.Any]): List[B]
}

object OrdersHandler {

  abstract class Insert[D[_]] {
    def insertNel(models: List[DBModel]): D[Int]
  }

  object Insert {

    final class Impl[D[_]: Monad] extends Insert[D] {

      def insertNel[A](xs: List[A])(insert: NonEmptyList[A] => D[Int]) =
        NonEmptyList.fromList(xs).fold(0.pure[D])(insert)

      def insertNel(models: List[DBModel], repoBundle: RepoBundle[D]): D[Int] = {
        val swaps   = models.collect { case s: DBSwap => s }
        val swapsV1 = models.collect { case s: DBSwapV1 => s }
        for {
          ss  <- insertNel(swaps)(repoBundle.swaps.insert)
          ss1 <- insertNel(swaps)(repoBundle.swapsv1.insert)
        } yield ss + ss1
      }
    }
  }

  def run[D[_]](handlers: List[OrdersHandler[DBModel]], orders: List[EvaluatedCFMMOrder.Any], insert: Insert[D]) =
    insert.insertNel {
      handlers.flatMap { handler =>
        handler.handle(orders)
      }
    }

  final class SwapV1Handler[F[_]: Applicative] extends OrdersHandler[DBSwap] {

    def handle(anyOrders: List[EvaluatedCFMMOrder.Any]): List[DBSwap] =
      anyOrders.collect { case EvaluatedCFMMOrder(o: Swap, Some(ev: SwapEvaluation), p) =>
        EvaluatedCFMMOrder(o, Some(ev), p).extract[DBSwap]
      }
  }

  final class SwapV2Handler[F[_]] extends OrdersHandler[DBSwapV1] {
    def handle(anyOrders: List[EvaluatedCFMMOrder.Any]): List[DBSwapV1] = ???
  }
}
