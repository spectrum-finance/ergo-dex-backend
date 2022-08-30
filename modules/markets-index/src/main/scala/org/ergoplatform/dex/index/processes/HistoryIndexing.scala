package org.ergoplatform.dex.index.processes

import cats.data.NonEmptyList
import cats.syntax.foldable._
import cats.syntax.option._
import cats.syntax.parallel._
import cats.{Foldable, Functor, Monad, Parallel}
import org.ergoplatform.dex.domain.amm.OrderEvaluation.{DepositEvaluation, RedeemEvaluation, SwapEvaluation}
import org.ergoplatform.dex.domain.amm.{Deposit, EvaluatedCFMMOrder, Redeem, Swap}
import org.ergoplatform.dex.index.db.Extract.syntax.ExtractOps
import org.ergoplatform.dex.index.db.models.{DBDeposit, DBRedeem, DBSwap}
import org.ergoplatform.dex.index.repositories.RepoBundle
import org.ergoplatform.dex.index.streaming.CFMMHistConsumer
import tofu.doobie.transactor.Txr
import tofu.logging.{Logging, Logs}
import tofu.streams.{Chunks, Evals}
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.streams.chunks._
import tofu.syntax.streams.evals._

trait HistoryIndexing[F[_]] {
  def run: F[Unit]
}

object HistoryIndexing {

  def make[
    I[_]: Functor,
    S[_]: Functor: Evals[*[_], F]: Chunks[*[_], C],
    F[_]: Monad: Parallel,
    D[_]: Monad,
    C[_]: Functor: Foldable
  ](implicit
    orders: CFMMHistConsumer[S, F],
    handlers: List[AnyOrdersHandler[D]],
    txr: Txr.Aux[F, D],
    logs: Logs[I, F]
  ): I[HistoryIndexing[S]] =
    logs.forService[HistoryIndexing[S]] map { implicit l =>
      new CFMMIndexing[S, F, D, C]
    }

  final class CFMMIndexing[
    S[_]: Functor: Evals[*[_], F]: Chunks[*[_], C],
    F[_]: Monad: Logging: Parallel,
    D[_]: Monad,
    C[_]: Functor: Foldable
  ](implicit
    orders: CFMMHistConsumer[S, F],
    handlers: List[AnyOrdersHandler[D]],
    txr: Txr.Aux[F, D]
  ) extends HistoryIndexing[S] {

    def run: S[Unit] =
      orders.stream.chunks
        .map { batch =>
          val batchMat = batch.toList
          batchMat.map(r => r.message) -> batchMat.lastOption.map(_.commit)
        }
        .evalTap { case (xs, _) => warn"[${xs.count(_.isEmpty)}] records discarded." }
        .evalMap { case (rs, commit) =>
          handlers
            .parTraverse { handler =>
              txr.trans(handler.handle(rs.flatten)): F[Int]
            }
            .map(_.sum)
            .flatMap { num =>
              info"[$num] orders indexed" *> commit.getOrElse(unit[F])
            }
        }
  }
}
