package org.ergoplatform.dex.index.processes

import cats.data.NonEmptyList
import cats.syntax.foldable._
import cats.syntax.traverse._
import cats.{Foldable, Functor, Monad}
import org.ergoplatform.dex.index.repositories.RepoBundle
import org.ergoplatform.dex.index.streaming.BlocksConsumer
import org.ergoplatform.ergo.services.explorer.ErgoExplorer
import tofu.doobie.transactor.Txr
import tofu.logging.{Logging, Logs}
import tofu.streams.{Chunks, Evals}
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.streams.chunks._
import org.ergoplatform.dex.index.db.Extract.syntax.ExtractOps
import org.ergoplatform.dex.index.db.models.DBBlock
import tofu.syntax.streams.evals._

trait BlockIndexing[F[_]] {
  def run: F[Unit]
}

object BlockIndexing {

  def make[
    I[_]: Functor,
    S[_]: Evals[*[_], F]: Chunks[*[_], C],
    F[_]: Monad,
    D[_]: Monad,
    C[_]: Functor: Foldable
  ](implicit
    blocks: BlocksConsumer[S, F],
    explorer: ErgoExplorer[F],
    repoBundle: RepoBundle[D],
    txr: Txr.Aux[F, D],
    logs: Logs[I, F]
  ): I[BlockIndexing[S]] =
    logs.forService[BlockIndexing[S]] map { implicit l =>
      new Indexing[S, F, D, C]
    }

  final class Indexing[
    S[_]: Evals[*[_], F]: Chunks[*[_], C],
    F[_]: Monad: Logging,
    D[_]: Monad,
    C[_]: Functor: Foldable
  ](implicit
    blocks: BlocksConsumer[S, F],
    explorer: ErgoExplorer[F],
    repos: RepoBundle[D],
    txr: Txr.Aux[F, D]
  ) extends BlockIndexing[S] {

    def run: S[Unit] =
      blocks.stream.chunks.evalMap { rs =>
        val blocks = rs.map(r => r.message.entity).toList
        def insertNel[A](xs: List[A])(insert: NonEmptyList[A] => D[Int]) =
          NonEmptyList.fromList(xs).fold(0.pure[D])(insert)
        val insert =
          insertNel(blocks)(xs => repos.blocks.insert(xs.map(c => c.extract[DBBlock])))
        txr.trans(insert) >>= { ls =>
          info"[$ls] blocks indexed"
        }
      }
  }
}
