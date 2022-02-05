package org.ergoplatform.dex.index.repositories

import cats.{FlatMap, Functor}
import cats.data.NonEmptyList
import cats.tagless.syntax.functorK._
import derevo.derive
import doobie.ConnectionIO
import doobie.util.log.LogHandler
import org.ergoplatform.dex.index.db.models.DBAssetInfo
import org.ergoplatform.dex.index.sql.AssetSql
import org.ergoplatform.ergo.TokenId
import tofu.doobie.LiftConnectionIO
import tofu.doobie.log.EmbeddableLogHandler
import tofu.higherKind.derived.representableK
import tofu.logging.Logs
import tofu.syntax.monadic._

@derive(representableK)
trait AssetRepo[F[_]] extends MonoRepo[DBAssetInfo, F] {

  /** Return only ids of existing tokens.
    */
  def existing(tokenIds: NonEmptyList[TokenId]): F[List[TokenId]]
}

object AssetRepo {

  def make[I[_]: Functor, D[_]: FlatMap: LiftConnectionIO](implicit
    elh: EmbeddableLogHandler[D],
    logs: Logs[I, D]
  ): I[AssetRepo[D]] =
    logs.forService[AssetRepo[D]].map { implicit l =>
      elh.embed(implicit lh => new Live().mapK(LiftConnectionIO[D].liftF))
    }

  final class Live(implicit lh: LogHandler) extends AssetRepo[ConnectionIO] {

    def insert(entities: NonEmptyList[DBAssetInfo]): ConnectionIO[Int] =
      AssetSql.insertNoConflict.updateMany(entities)

    def existing(tokenIds: NonEmptyList[TokenId]): ConnectionIO[List[TokenId]] =
      AssetSql.existing(tokenIds).to[List]
  }
}
