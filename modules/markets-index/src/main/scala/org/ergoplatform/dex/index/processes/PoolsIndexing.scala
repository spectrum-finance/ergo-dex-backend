package org.ergoplatform.dex.index.processes

import cats.data.NonEmptyList
import cats.syntax.foldable._
import cats.{Foldable, Functor, Monad}
import org.ergoplatform.dex.index.db.Extract.syntax.ExtractOps
import org.ergoplatform.dex.index.db.models.{DBPool, PoolAssets}
import org.ergoplatform.dex.index.repos.RepoBundle
import org.ergoplatform.dex.index.streaming.CFMMPoolsConsumer
import tofu.doobie.transactor.Txr
import tofu.logging.{Logging, Logs}
import tofu.streams.{Chunks, Evals}
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.streams.chunks._
import tofu.syntax.streams.evals._

trait PoolsIndexing[F[_]] {
  def run: F[Unit]
}

object PoolsIndexing {

  def make[
    I[_]: Functor,
    S[_]: Evals[*[_], F]: Chunks[*[_], C],
    F[_]: Monad,
    D[_]: Monad,
    C[_]: Functor: Foldable
  ](implicit
    pools: CFMMPoolsConsumer[S, F],
    repoBundle: RepoBundle[D],
    txr: Txr.Aux[F, D],
    logs: Logs[I, F]
  ): I[PoolsIndexing[S]] =
    logs.forService[PoolsIndexing[S]] map { implicit l =>
      new CFMMIndexing[S, F, D, C]
    }

  final class CFMMIndexing[
    S[_]: Evals[*[_], F]: Chunks[*[_], C],
    F[_]: Monad: Logging,
    D[_]: Monad,
    C[_]: Functor: Foldable
  ](implicit
    pools: CFMMPoolsConsumer[S, F],
    repos: RepoBundle[D],
    txr: Txr.Aux[F, D]
  ) extends PoolsIndexing[S] {

    def run: S[Unit] =
      pools.stream.chunks.evalMap { rs =>
        val poolSnapshots = rs.map(r => r.message.confirmed).toList
        def insertNel[A](xs: List[A])(insert: NonEmptyList[A] => D[Int]) =
          NonEmptyList.fromList(xs).fold(0.pure[D])(insert)
        val insert =
          for {
            pn <- insertNel(poolSnapshots)(xs => repos.pools.insert(xs.map(_.extract[DBPool])))
            an <- insertNel(poolSnapshots) { xs =>
                    repos.assets.insert(xs.flatMap { p =>
                      val (lp, x, y) = p.extract[PoolAssets]
                      NonEmptyList.of(lp, x, y)
                    })
                  }
          } yield (pn, an)
        txr.trans(insert) >>= { case (pn, an) =>
          info"[$pn] pool snapshots indexed" >> info"[$an] assets indexed"
        }
      }
  }
}
