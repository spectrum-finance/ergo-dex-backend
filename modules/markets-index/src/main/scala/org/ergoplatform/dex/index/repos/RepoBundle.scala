package org.ergoplatform.dex.index.repos

import cats.{~>, FlatMap}
import cats.tagless.FunctorK
import cats.tagless.syntax.functorK._
import tofu.doobie.LiftConnectionIO
import tofu.doobie.log.EmbeddableLogHandler
import tofu.logging.Logs
import tofu.syntax.monadic._

final case class RepoBundle[F[_]](
  outputs: OutputsRepo[F],
  assets: AssetsRepo[F],
  orders: CFMMOrdersRepo[F],
  pools: PoolsRepo[F]
)

object RepoBundle {

  implicit val functorK: FunctorK[RepoBundle] =
    new FunctorK[RepoBundle] {

      def mapK[F[_], G[_]](af: RepoBundle[F])(fk: F ~> G): RepoBundle[G] =
        RepoBundle(af.outputs.mapK(fk), af.assets.mapK(fk), af.orders.mapK(fk), af.pools.mapK(fk))
    }

  def make[I[_]: FlatMap, D[_]: FlatMap: LiftConnectionIO](implicit
    elh: EmbeddableLogHandler[D],
    logs: Logs[I, D]
  ): I[RepoBundle[D]] =
    for {
      outputs <- OutputsRepo.make[I, D]
      assets  <- AssetsRepo.make[I, D]
      orders  <- CFMMOrdersRepo.make[I, D]
    } yield RepoBundle(outputs, assets, orders, ???)
}
