package org.ergoplatform.dex.index.processes

import cats.data.NonEmptyList
import cats.syntax.foldable._
import cats.{Foldable, Functor, Monad}
import org.ergoplatform.dex.domain.amm.{Deposit, Redeem, Swap}
import org.ergoplatform.dex.index.db.models.{depositToDb, outputToDb, redeemToDb, swapToDb}
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
  ](implicit orders: CFMMConsumer[S, F],
    repoBundle: RepoBundle[F],
    logs: Logs[I, F]): I[Indexing[S]] =
    logs.forService[Indexing[S]] map { implicit l =>
      new CFMMIndexing[S, F, C]
    }

  final class CFMMIndexing[
    S[_]: Evals[*[_], F]: Chunks[*[_], C],
    F[_]: Monad: Logging,
    C[_]: Functor: Foldable
  ](implicit
    orders: CFMMConsumer[S, F],
    repoBundle: RepoBundle[F]
  ) extends Indexing[S] {

    def run: S[Unit] =
      orders.stream.chunks.evalMap { rs =>
        val orders = rs.map(r => r.message).toList
        val (swaps, others) = orders.partitionEither {
          case o: Swap => Left(o)
          case o       => Right(o)
        }
        val (deposits, redeems) = others.partitionEither {
          case o: Deposit => Left(o)
          case o: Redeem  => Right(o)
        }
        val outputs = orders.map(_.box)
        def insertMaybe[A](xs: List[A])(insert: NonEmptyList[A] => F[Int]) =
          NonEmptyList.fromList(xs).fold(0.pure[F])(insert)
        for {
          ss <- insertMaybe(swaps)(xs => repoBundle.CFMMOrdersRepo.insertSwaps(xs.map(swapToDb)))
          ds <- insertMaybe(deposits)(xs => repoBundle.CFMMOrdersRepo.insertDeposits(xs.map(depositToDb)))
          rs <- insertMaybe(redeems)(xs => repoBundle.CFMMOrdersRepo.insertRedeems(xs.map(redeemToDb)))
          _ <- insertMaybe(outputs.map(outputToDb))(xs => repoBundle.outputsRepo.insertOutputs(xs))
          _ <- insertMaybe(outputs.flatMap(_.assets))(xs => repoBundle.assetsRepo.insertAssets(xs))
          _  <- info"[${ss + ds + rs}}] orders indexed"
        } yield ()
      }
  }
}
