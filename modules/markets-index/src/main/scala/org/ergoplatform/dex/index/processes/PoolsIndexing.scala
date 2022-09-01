package org.ergoplatform.dex.index.processes

import cats.data.NonEmptyList
import cats.implicits.catsSyntaxSemigroup
import cats.syntax.foldable._
import cats.syntax.traverse._
import cats.{Foldable, Functor, Monad}
import derevo.derive
import org.ergoplatform.dex.index.configs.PoolsIndexingConfig
import org.ergoplatform.dex.domain.amm.CFMMPool
import org.ergoplatform.dex.index.db.Extract.syntax.ExtractOps
import org.ergoplatform.dex.index.db.models.{DBAssetInfo, DBPoolSnapshot}
import org.ergoplatform.dex.index.repositories.RepoBundle
import org.ergoplatform.dex.index.streaming.CFMMPoolsConsumer
import org.ergoplatform.dex.protocol.constants.ErgoAssetId
import org.ergoplatform.ergo.TokenId
import org.ergoplatform.ergo.errors.ResponseError
import org.ergoplatform.ergo.services.explorer.ErgoExplorer
import org.ergoplatform.ergo.services.explorer.models.TokenInfo.ErgoTokenInfo
import org.ergoplatform.ergo.state.ConfirmedIndexed
import retry.RetryPolicies._
import retry.{RetryDetails, Sleep, retryingOnSomeErrors}
import tofu.MonadThrow
import tofu.doobie.transactor.Txr
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.streams.{Chunks, Evals}
import tofu.syntax.embed._
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.streams.chunks._
import tofu.syntax.streams.evals._

@derive(representableK)
trait PoolsIndexing[F[_]] {
  def run: F[Unit]
}

object PoolsIndexing {

  def make[
    I[_]: Functor,
    S[_]: Monad: Evals[*[_], F]: Chunks[*[_], C]: PoolsIndexingConfig.Has,
    F[_]: Monad: Sleep: MonadThrow,
    D[_]: Monad,
    C[_]: Functor: Foldable
  ](implicit
    pools: CFMMPoolsConsumer[S, F],
    explorer: ErgoExplorer[F],
    repoBundle: RepoBundle[D],
    txr: Txr.Aux[F, D],
    logs: Logs[I, F]
  ): I[PoolsIndexing[S]] =
    logs.forService[PoolsIndexing[S]] map { implicit l =>
      (PoolsIndexingConfig.access map (conf => new CFMMIndexing[S, F, D, C](conf): PoolsIndexing[S])).embed
    }

  final class CFMMIndexing[
    S[_]: Evals[*[_], F]: Chunks[*[_], C],
    F[_]: Monad: Logging: Sleep: MonadThrow,
    D[_]: Monad,
    C[_]: Functor: Foldable
  ](conf: PoolsIndexingConfig)(implicit
    pools: CFMMPoolsConsumer[S, F],
    explorer: ErgoExplorer[F],
    repos: RepoBundle[D],
    txr: Txr.Aux[F, D]
  ) extends PoolsIndexing[S] {

    def run: S[Unit] =
      pools.stream.chunks.evalMap { rs =>
        val batch         = rs.toList
        val poolSnapshots = batch.map(_.message)
        val assets        = poolSnapshots.flatMap(p => List(p.entity.lp.id, p.entity.x.id, p.entity.y.id)).distinct
        val commit        = batch.lastOption.map(_.commit)

        val retryPolicy = limitRetries[F](conf.limitRetries) |+| constantDelay[F](conf.retryDelay)
        val isResponseError: Throwable => Boolean = {
          case _: ResponseError => true
          case _                => false
        }
        val resolveNewAssets =
          for {
            existingAssets <-
              txr.trans(NonEmptyList.fromList(assets).fold(List.empty[TokenId].pure[D])(repos.assets.existing))
            unknownAssets = assets.diff(existingAssets)
            assetsInfo <- unknownAssets
                            .filterNot(_ == ErgoAssetId)
                            .flatTraverse(tknId =>
                              retryingOnSomeErrors(
                                retryPolicy,
                                isResponseError,
                                (_: Throwable, _: RetryDetails) =>
                                  info"Failed to find token $tknId. Retrying..."
                              )(explorer.getTokenInfo(tknId)).map(_.toList)
                            )
            nativeAsset = if (unknownAssets.contains(ErgoAssetId)) List(ErgoTokenInfo) else Nil
          } yield nativeAsset ++ assetsInfo

        def insertNel[A](xs: List[A])(insert: NonEmptyList[A] => D[Int]) =
          NonEmptyList.fromList(xs).fold(0.pure[D])(insert)

        for {
          newAssets <- resolveNewAssets
          insert =
            for {
              pn <- insertNel(poolSnapshots)(xs => repos.pools.insert(xs.map(_.extract[DBPoolSnapshot])))
              an <- insertNel(newAssets)(xs => repos.assets.insert(xs.map(_.extract[DBAssetInfo])))
            } yield (pn, an)
          (pn, an) <- txr.trans(insert)
          _        <- info"[$pn] pool snapshots indexed" >> info"[$an] assets indexed"
          _        <- commit.getOrElse(unit[F])
        } yield ()
      }
  }
}
