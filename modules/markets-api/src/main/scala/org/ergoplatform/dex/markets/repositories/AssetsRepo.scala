package org.ergoplatform.dex.markets.repositories

import cats.{FlatMap, Functor}
import cats.tagless.syntax.functorK._
import cats.data.NonEmptyList
import derevo.derive
import doobie.ConnectionIO
import org.ergoplatform.dex.markets.db.models.AssetInfo
import org.ergoplatform.dex.markets.sql.AssetQueries
import tofu.doobie.LiftConnectionIO
import tofu.doobie.log.EmbeddableLogHandler
import tofu.higherKind.derived.representableK
import tofu.logging.Logs
import tofu.syntax.monadic._

@derive(representableK)
trait AssetsRepo[F[_]] {

  def insert(xs: NonEmptyList[AssetInfo]): F[Int]
}

object AssetsRepo {

  def make[I[_]: Functor, D[_]: FlatMap: LiftConnectionIO](implicit
    elh: EmbeddableLogHandler[D],
    logs: Logs[I, D]
  ): I[AssetsRepo[D]] =
    logs.forService[AssetsRepo[D]].map { implicit l =>
      elh.embed(implicit lh => new Live(new AssetQueries()).mapK(LiftConnectionIO[D].liftF))
    }

  final class Live(qs: AssetQueries) extends AssetsRepo[ConnectionIO] {

    def insert(xs: NonEmptyList[AssetInfo]): ConnectionIO[Int] =
      qs.insertNoConflict[AssetInfo].updateMany(xs)
  }
}
