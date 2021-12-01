package org.ergoplatform.dex.index.processes

import cats.data.NonEmptyList
import cats.syntax.foldable._
import cats.{Applicative, Foldable, Functor, Monad}
import org.ergoplatform.dex.domain.amm.{Deposit, Redeem, Swap}
import org.ergoplatform.dex.index.db.DBView.syntax.DBViewOps
import org.ergoplatform.dex.index.db.models.{DBDeposit, DBOutput, DBRedeem, DBSwap}
import org.ergoplatform.dex.index.repos.RepoBundle
import org.ergoplatform.dex.index.streaming.CFMMConsumer
import tofu.doobie.transactor.Txr
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
    D[_]: Monad,
    C[_]: Functor: Foldable
  ](implicit
    orders: CFMMConsumer[S, F],
    repoBundle: RepoBundle[D],
    txr: Txr.Aux[F, D],
    logs: Logs[I, F]
  ): I[Indexing[S]] =
    logs.forService[Indexing[S]] map { implicit l =>
      new CFMMIndexing[S, F, D, C]
    }

  final class CFMMIndexing[
    S[_]: Evals[*[_], F]: Chunks[*[_], C],
    F[_]: Monad: Logging,
    D[_]: Monad,
    C[_]: Functor: Foldable
  ](implicit
    orders: CFMMConsumer[S, F],
    repos: RepoBundle[D],
    txr: Txr.Aux[F, D]
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
        def insertNel[A](xs: List[A])(insert: NonEmptyList[A] => D[Int]) =
          NonEmptyList.fromList(xs).fold(0.pure[D])(insert)
        val insert =
          for {
            ss <- insertNel(swaps)(xs => repos.orders.insertSwaps(xs.map(_.dbView[DBSwap])))
            ds <- insertNel(deposits)(xs => repos.orders.insertDeposits(xs.map(_.dbView[DBDeposit])))
            rs <- insertNel(redeems)(xs => repos.orders.insertRedeems(xs.map(_.dbView[DBRedeem])))
            _  <- insertNel(outputs.map(_.dbView[DBOutput]))(xs => repos.outputs.insertOutputs(xs))
            _  <- insertNel(outputs.flatMap(_.assets))(xs => repos.assets.insertAssets(xs))
          } yield ss + ds + rs
        txr.trans(insert) >>= (n => info"[$n}] orders indexed")
      }
  }
}
