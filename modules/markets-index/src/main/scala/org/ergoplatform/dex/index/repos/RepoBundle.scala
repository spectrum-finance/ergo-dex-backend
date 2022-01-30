package org.ergoplatform.dex.index.repos

import cats.{~>, FlatMap}
import cats.tagless.FunctorK
import cats.tagless.syntax.functorK._
import org.ergoplatform.dex.index.db.models.{DBAssetInfo, DBDeposit, DBLiquidityLock, DBPool, DBRedeem, DBSwap}
import tofu.doobie.LiftConnectionIO
import tofu.doobie.log.EmbeddableLogHandler
import tofu.logging.Logs
import tofu.syntax.monadic._

final case class RepoBundle[F[_]](
  swaps: MonoRepo[DBSwap, F],
  deposits: MonoRepo[DBDeposit, F],
  redeems: MonoRepo[DBRedeem, F],
  pools: MonoRepo[DBPool, F],
  assets: MonoRepo[DBAssetInfo, F],
  locks: MonoRepo[DBLiquidityLock, F]
)

object RepoBundle {

  implicit val functorK: FunctorK[RepoBundle] =
    new FunctorK[RepoBundle] {

      def mapK[F[_], G[_]](af: RepoBundle[F])(fk: F ~> G): RepoBundle[G] =
        RepoBundle(
          af.swaps.mapK(fk),
          af.deposits.mapK(fk),
          af.redeems.mapK(fk),
          af.pools.mapK(fk),
          af.assets.mapK(fk),
          af.locks.mapK(fk)
        )
    }

  def make[I[_]: FlatMap, D[_]: FlatMap: LiftConnectionIO](implicit
    elh: EmbeddableLogHandler[D],
    logs: Logs[I, D]
  ): I[RepoBundle[D]] =
    for {
      swaps    <- MonoRepo.make[I, D, DBSwap]
      deposits <- MonoRepo.make[I, D, DBDeposit]
      redeems  <- MonoRepo.make[I, D, DBRedeem]
      pools    <- MonoRepo.make[I, D, DBPool]
      assets   <- MonoRepo.make[I, D, DBAssetInfo]
      locks    <- MonoRepo.make[I, D, DBLiquidityLock]
    } yield RepoBundle(swaps, deposits, redeems, pools, assets, locks)
}
