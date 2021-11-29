package org.ergoplatform.dex.index.repos

import cats.data.NonEmptyList
import cats.{FlatMap, Functor}
import doobie.ConnectionIO
import doobie.util.log.LogHandler
import org.ergoplatform.dex.index.sql.AssetsSql
import org.ergoplatform.ergo.models.BoxAsset
import tofu.doobie.LiftConnectionIO
import tofu.doobie.log.EmbeddableLogHandler
import tofu.logging.Logs
import tofu.syntax.monadic._
import cats.tagless.syntax.functorK._
import tofu.higherKind.derived.representableK
import derevo.derive

@derive(representableK)
trait AssetsRepo[F[_]] {
  def insertAssets(assets: NonEmptyList[BoxAsset]): F[Int]
}

object AssetsRepo {

  def make[I[_]: Functor, D[_]: FlatMap: LiftConnectionIO](implicit
    elh: EmbeddableLogHandler[D],
    logs: Logs[I, D]
  ): I[AssetsRepo[D]] =
    logs.forService[AssetsRepo[D]].map { implicit l =>
      elh.embed(implicit lh => new Live().mapK(LiftConnectionIO[D].liftF))
    }

  final class Live(implicit lh: LogHandler) extends AssetsRepo[ConnectionIO] {

    override def insertAssets(assets: NonEmptyList[BoxAsset]): ConnectionIO[Int] =
      AssetsSql.insert[BoxAsset].updateMany(assets)
  }

}
