package org.ergoplatform.dex.index.repos

import cats.data.NonEmptyList
import cats.{FlatMap, Functor}
import derevo.derive
import doobie.ConnectionIO
import doobie.util.log.LogHandler
import org.ergoplatform.dex.index.db.models.DBOutput
import org.ergoplatform.dex.index.sql.OutputsSql
import tofu.doobie.LiftConnectionIO
import tofu.doobie.log.EmbeddableLogHandler
import tofu.higherKind.derived.representableK
import tofu.logging.Logs
import tofu.syntax.monadic._
import org.ergoplatform.dex.index.db.instances._
import cats.tagless.syntax.functorK._

@derive(representableK)
trait OutputsRepo[F[_]] {
  def insertOutputs(outputs: NonEmptyList[DBOutput]): F[Int]
}

object OutputsRepo {

  def make[I[_]: Functor, D[_]: FlatMap: LiftConnectionIO](implicit
    elh: EmbeddableLogHandler[D],
    logs: Logs[I, D]
  ): I[OutputsRepo[D]] =
    logs.forService[OutputsRepo[D]].map { implicit l =>
      elh.embed(implicit lh => new Live().mapK(LiftConnectionIO[D].liftF))
    }

  final class Live(implicit lh: LogHandler) extends OutputsRepo[ConnectionIO] {

    def insertOutputs(outputs: NonEmptyList[DBOutput]): ConnectionIO[Int] =
      OutputsSql.insert[DBOutput].updateMany(outputs)
  }
}
