package org.ergoplatform.dex.index.processes

import cats.data.NonEmptyList
import cats.syntax.foldable._
import cats.{Foldable, Functor, Monad}
import org.ergoplatform.dex.domain.amm.{Deposit, Redeem, Swap}
import org.ergoplatform.dex.index.db.DBView.syntax.DBViewOps
import org.ergoplatform.dex.index.db.models.{DBDeposit, DBOutput, DBRedeem, DBSwap}
import org.ergoplatform.dex.index.repos.RepoBundle
import org.ergoplatform.dex.index.streaming.CFMMConsumer
import tofu.logging.{Logging, Logs}
import tofu.streams.{Chunks, Evals}
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.streams.chunks._
import tofu.syntax.streams.evals._

trait Indexing[F[_]] {
  def run: F[Unit]
}

object Indexing {

  def make[
    I[_]: Functor,
    S[_]: Evals[*[_], F]: Chunks[*[_], C],
    F[_]: Monad,
    C[_]: Functor: Foldable
  ](implicit orders: CFMMConsumer[S, F], repoBundle: RepoBundle[F], logs: Logs[I, F]): I[Indexing[S]] =
    logs.forService[Indexing[S]] map { implicit l =>
      new CFMMIndexing[S, F, C]
    }

  final class CFMMIndexing[
    S[_]: Evals[*[_], F]: Chunks[*[_], C],
    F[_]: Monad: Logging,
    C[_]: Functor: Foldable
  ](implicit
    orders: CFMMConsumer[S, F],
    repos: RepoBundle[F]
  ) extends Indexing[S] {

    def run: S[Unit] =
      orders.stream.chunks.evalMap { rs =>
        val orders = rs.map(r => r.message).toList
        val (swaps, others) = orders.partitionEither {
          case o: Swap    => Left(o)
          case o: Deposit => Right(Left(o))
          case o: Redeem  => Right(Right(o))
        }
        val (deposits, redeems) = others.partitionEither(identity)
        val outputs             = orders.map(_.box)
        def insertMaybe[A](xs: List[A])(insert: NonEmptyList[A] => F[Int]) =
          NonEmptyList.fromList(xs).fold(0.pure[F])(insert)
        for {
          ss <- insertMaybe(swaps)(xs => repos.orders.insertSwaps(xs.map(_.viewDB[DBSwap])))
          ds <- insertMaybe(deposits)(xs => repos.orders.insertDeposits(xs.map(_.viewDB[DBDeposit])))
          rs <- insertMaybe(redeems)(xs => repos.orders.insertRedeems(xs.map(_.viewDB[DBRedeem])))
          _  <- insertMaybe(outputs.map(_.viewDB[DBOutput]))(xs => repos.outputs.insertOutputs(xs))
          _  <- insertMaybe(outputs.flatMap(_.assets))(xs => repos.assets.insertAssets(xs))
          _  <- info"[${ss + ds + rs}}] orders indexed"
        } yield ()
      }
  }
}
