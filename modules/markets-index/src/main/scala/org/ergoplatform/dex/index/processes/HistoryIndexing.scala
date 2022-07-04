package org.ergoplatform.dex.index.processes

import cats.data.NonEmptyList
import cats.syntax.foldable._
import cats.syntax.option._
import cats.{Foldable, Functor, Monad}
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
    F[_]: Monad,
    D[_]: Monad,
    C[_]: Functor: Foldable
  ](implicit
    orders: CFMMHistConsumer[S, F],
    repoBundle: RepoBundle[D],
    txr: Txr.Aux[F, D],
    logs: Logs[I, F]
  ): I[HistoryIndexing[S]] =
    logs.forService[HistoryIndexing[S]] map { implicit l =>
      new CFMMIndexing[S, F, D, C]
    }

  final class CFMMIndexing[
    S[_]: Functor: Evals[*[_], F]: Chunks[*[_], C],
    F[_]: Monad: Logging,
    D[_]: Monad,
    C[_]: Functor: Foldable
  ](implicit
    orders: CFMMHistConsumer[S, F],
    repos: RepoBundle[D],
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
          val orders = rs.flatten
          val (swaps, others) = orders.partitionEither {
            case EvaluatedCFMMOrder(o: Swap, Some(ev: SwapEvaluation), p) =>
              Left(EvaluatedCFMMOrder(o, Some(ev), p).extract[DBSwap])
            case EvaluatedCFMMOrder(o: Swap, _, p) =>
              Left(EvaluatedCFMMOrder(o, none[SwapEvaluation], p).extract[DBSwap])
            case EvaluatedCFMMOrder(o: Deposit, Some(ev: DepositEvaluation), p) =>
              Right(Left(EvaluatedCFMMOrder(o, Some(ev), p).extract[DBDeposit]))
            case EvaluatedCFMMOrder(o: Deposit, _, p) =>
              Right(Left(EvaluatedCFMMOrder(o, none[DepositEvaluation], p).extract[DBDeposit]))
            case EvaluatedCFMMOrder(o: Redeem, Some(ev: RedeemEvaluation), p) =>
              Right(Right(EvaluatedCFMMOrder(o, Some(ev), p).extract[DBRedeem]))
            case EvaluatedCFMMOrder(o: Redeem, _, p) =>
              Right(Right(EvaluatedCFMMOrder(o, none[RedeemEvaluation], p).extract[DBRedeem]))
          }
          val (deposits, redeems) = others.partitionEither(identity)
          def insertNel[A](xs: List[A])(insert: NonEmptyList[A] => D[Int]) =
            NonEmptyList.fromList(xs).fold(0.pure[D])(insert)
          val insert =
            for {
              ss <- insertNel(swaps)(repos.swaps.insert)
              ds <- insertNel(deposits)(repos.deposits.insert)
              rs <- insertNel(redeems)(repos.redeems.insert)
            } yield ss + ds + rs
          for {
            n <- txr.trans(insert)
            _ <- info"[$n] orders indexed"
            _ <- commit.getOrElse(unit[F])
          } yield ()
        }
  }
}
