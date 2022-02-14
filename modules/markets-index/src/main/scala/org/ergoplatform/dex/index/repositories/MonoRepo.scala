package org.ergoplatform.dex.index.repositories

import cats.data.NonEmptyList
import cats.tagless.syntax.functorK._
import cats.{FlatMap, Functor}
import doobie.ConnectionIO
import doobie.util.Write
import doobie.util.log.LogHandler
import org.ergoplatform.common.sql.QuerySet
import tofu.doobie.LiftConnectionIO
import tofu.doobie.log.EmbeddableLogHandler
import tofu.higherKind.RepresentableK
import tofu.logging.Logs
import tofu.syntax.monadic._

trait MonoRepo[T, F[_]] {

  def insert(entities: NonEmptyList[T]): F[Int]
}

object MonoRepo {

  implicit def repK[T]: RepresentableK[MonoRepo[T, *[_]]] = {
    type Repr[F[_]] = MonoRepo[T, F]
    tofu.higherKind.derived.genRepresentableK[Repr]
  }

  def make[I[_]: Functor, D[_]: FlatMap: LiftConnectionIO, T: Write](implicit
    sql: QuerySet[T],
    elh: EmbeddableLogHandler[D],
    logs: Logs[I, D]
  ): I[MonoRepo[T, D]] =
    logs.forService[MonoRepo[T, D]].map { implicit l =>
      elh.embed(implicit lh => new Live().mapK(LiftConnectionIO[D].liftF))
    }

  final class Live[T: Write](implicit sql: QuerySet[T], lh: LogHandler) extends MonoRepo[T, ConnectionIO] {

    def insert(entities: NonEmptyList[T]): ConnectionIO[Int] =
      sql.insertNoConflict.updateMany(entities)
  }
}
