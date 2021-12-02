package org.ergoplatform.dex.index.processes

import cats.data.NonEmptyList
import cats.syntax.foldable._
import cats.syntax.option._
import cats.{Foldable, Functor, Monad}
import org.ergoplatform.dex.domain.amm.OrderEvaluation.{DepositEvaluation, RedeemEvaluation, SwapEvaluation}
import org.ergoplatform.dex.domain.amm.{Deposit, EvaluatedCFMMOrder, Redeem, Swap}
import org.ergoplatform.dex.index.db.DBView.syntax.DBViewOps
import org.ergoplatform.dex.index.db.models.{DBDeposit, DBOutput, DBRedeem, DBSwap}
import org.ergoplatform.dex.index.repos.RepoBundle
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
    S[_]: Evals[*[_], F]: Chunks[*[_], C],
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
    S[_]: Evals[*[_], F]: Chunks[*[_], C],
    F[_]: Monad: Logging,
    D[_]: Monad,
    C[_]: Functor: Foldable
  ](implicit
    orders: CFMMHistConsumer[S, F],
    repos: RepoBundle[D],
    txr: Txr.Aux[F, D]
  ) extends HistoryIndexing[S] {

    def run: S[Unit] =
      orders.stream.chunks.evalMap { rs =>
        val orders = rs.map(r => r.message).toList
        val (swaps, others) = orders.partitionEither {
          case EvaluatedCFMMOrder(o: Swap, Some(ev: SwapEvaluation), p) =>
            Left(EvaluatedCFMMOrder(o, Some(ev), p).dbView[DBSwap])
          case EvaluatedCFMMOrder(o: Swap, _, p) =>
            Left(EvaluatedCFMMOrder(o, none[SwapEvaluation], p).dbView[DBSwap])
          case EvaluatedCFMMOrder(o: Deposit, Some(ev: DepositEvaluation), p) =>
            Right(Left(EvaluatedCFMMOrder(o, Some(ev), p).dbView[DBDeposit]))
          case EvaluatedCFMMOrder(o: Deposit, _, p) =>
            Right(Left(EvaluatedCFMMOrder(o, none[DepositEvaluation], p).dbView[DBDeposit]))
          case EvaluatedCFMMOrder(o: Redeem, Some(ev: RedeemEvaluation), p) =>
            Right(Right(EvaluatedCFMMOrder(o, Some(ev), p).dbView[DBRedeem]))
          case EvaluatedCFMMOrder(o: Redeem, _, p) =>
            Right(Right(EvaluatedCFMMOrder(o, none[RedeemEvaluation], p).dbView[DBRedeem]))
        }
        val (deposits, redeems) = others.partitionEither(identity)
        val outputs             = orders.map(_.order.box)
        def insertNel[A](xs: List[A])(insert: NonEmptyList[A] => D[Int]) =
          NonEmptyList.fromList(xs).fold(0.pure[D])(insert)
        val insert =
          for {
            ss <- insertNel(swaps)(repos.orders.insertSwaps)
            ds <- insertNel(deposits)(repos.orders.insertDeposits)
            rs <- insertNel(redeems)(repos.orders.insertRedeems)
            _  <- insertNel(outputs.map(_.dbView[DBOutput]))(xs => repos.outputs.insertOutputs(xs))
            _  <- insertNel(outputs.flatMap(_.assets))(xs => repos.assets.insertAssets(xs))
          } yield ss + ds + rs
        txr.trans(insert) >>= (n => info"[$n] orders indexed")
      }
  }
}
