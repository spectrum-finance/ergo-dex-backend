package org.ergoplatform.dex.index.processes

import cats.data.NonEmptyList
import cats.syntax.foldable._
import cats.{Foldable, Functor, Monad}
import org.ergoplatform.dex.index.db.Extract.syntax.ExtractOps
import org.ergoplatform.dex.index.db.models.DBLiquidityLock
import org.ergoplatform.dex.index.repositories.RepoBundle
import org.ergoplatform.dex.index.streaming.LqLocksConsumer
import tofu.doobie.transactor.Txr
import tofu.logging.{Logging, Logs}
import tofu.streams.{Chunks, Evals}
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.streams.chunks._
import tofu.syntax.streams.evals._

trait LocksIndexing[F[_]] {
  def run: F[Unit]
}

object LocksIndexing {

  def make[
    I[_]: Functor,
    S[_]: Evals[*[_], F]: Chunks[*[_], C],
    F[_]: Monad,
    D[_]: Monad,
    C[_]: Functor: Foldable
  ](implicit
    pools: LqLocksConsumer[S, F],
    repoBundle: RepoBundle[D],
    txr: Txr.Aux[F, D],
    logs: Logs[I, F]
  ): I[LocksIndexing[S]] =
    logs.forService[LocksIndexing[S]] map { implicit l =>
      new Live[S, F, D, C]
    }

  final class Live[
    S[_]: Evals[*[_], F]: Chunks[*[_], C],
    F[_]: Monad: Logging,
    D[_]: Monad,
    C[_]: Functor: Foldable
  ](implicit
    locks: LqLocksConsumer[S, F],
    repos: RepoBundle[D],
    txr: Txr.Aux[F, D]
  ) extends LocksIndexing[S] {

    def run: S[Unit] =
      locks.stream.chunks.evalMap { rs =>
        val locks = rs.map(r => r.message.confirmed).toList
        def insertNel[A](xs: List[A])(insert: NonEmptyList[A] => D[Int]) =
          NonEmptyList.fromList(xs).fold(0.pure[D])(insert)
        val insert =
          insertNel(locks)(xs => repos.locks.insert(xs.map(_.extract[DBLiquidityLock])))
        txr.trans(insert) >>= { ls =>
          info"[$ls] locks indexed"
        }
      }
  }
}
