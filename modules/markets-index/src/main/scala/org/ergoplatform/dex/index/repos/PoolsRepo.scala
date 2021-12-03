package org.ergoplatform.dex.index.repos

import cats.data.NonEmptyList
import cats.{FlatMap, Functor}
import derevo.derive
import doobie.ConnectionIO
import doobie.util.log.LogHandler
import org.ergoplatform.dex.index.db.models.DBCFMMPool
import org.ergoplatform.dex.index.sql.CFMMPoolSql
import tofu.doobie.LiftConnectionIO
import tofu.doobie.log.EmbeddableLogHandler
import tofu.higherKind.derived.representableK
import tofu.logging.Logs
import tofu.syntax.monadic._
import cats.tagless.syntax.functorK._

@derive(representableK)
trait PoolsRepo[F[_]] {

  def insert(pools: NonEmptyList[DBCFMMPool]): F[Int]
}

object PoolsRepo {

  def make[I[_]: Functor, D[_]: FlatMap: LiftConnectionIO](implicit
    elh: EmbeddableLogHandler[D],
    logs: Logs[I, D]
  ): I[PoolsRepo[D]] =
    logs.forService[PoolsRepo[D]].map { implicit l =>
      elh.embed(implicit lh => new Live().mapK(LiftConnectionIO[D].liftF))
    }

  final class Live(implicit lh: LogHandler) extends PoolsRepo[ConnectionIO] {

    override def insert(pools: NonEmptyList[DBCFMMPool]): ConnectionIO[Int] =
      CFMMPoolSql.insert[DBCFMMPool].updateMany(pools)

  }
}
