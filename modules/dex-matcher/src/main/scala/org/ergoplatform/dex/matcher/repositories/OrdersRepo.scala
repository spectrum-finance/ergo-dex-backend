package org.ergoplatform.dex.matcher.repositories

import cats.data.NonEmptyList
import cats.tagless.syntax.functorK._
import cats.{Apply, FlatMap, Functor}
import derevo.derive
import doobie.ConnectionIO
import doobie.util.log.LogHandler
import org.ergoplatform.dex.domain.orderbook.Order.{AnyOrder, Ask, Bid}
import org.ergoplatform.dex.{OrderId, PairId}
import tofu.doobie.LiftConnectionIO
import tofu.doobie.log.EmbeddableLogHandler
import tofu.higherKind.Mid
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._

@derive(representableK)
trait OrdersRepo[D[_]] {

  def getBuyWall(pairId: PairId, limit: Long): D[List[Bid]]

  def getSellWall(pairId: PairId, limit: Long): D[List[Ask]]

  def insert(orders: NonEmptyList[AnyOrder]): D[Unit]

  def remove(ids: NonEmptyList[OrderId]): D[Unit]
}

object OrdersRepo {

  def make[I[_]: Functor, D[_]: FlatMap: LiftConnectionIO](implicit
    elh: EmbeddableLogHandler[D],
    logs: Logs[I, D]
  ): I[OrdersRepo[D]] =
    logs.forService[OrdersRepo[D]].map { implicit l =>
      elh.embed(implicit lh => new OrdersRepoTracing[D] attach new Live().mapK(LiftConnectionIO[D].liftF))
    }

  final class Live(implicit lh: LogHandler) extends OrdersRepo[ConnectionIO] {

    import org.ergoplatform.dex.matcher.sql.{ordersSql => sql}

    def getBuyWall(pairId: PairId, limit: Long): ConnectionIO[List[Bid]] =
      sql.getBuyWall(pairId).stream.take(limit).compile.toList

    def getSellWall(pairId: PairId, limit: Long): ConnectionIO[List[Ask]] =
      sql.getSellWall(pairId).stream.take(limit).compile.toList

    def insert(orders: NonEmptyList[AnyOrder]): ConnectionIO[Unit] =
      sql.insert[AnyOrder].updateMany(orders).map(_ => ())

    def remove(ids: NonEmptyList[OrderId]): ConnectionIO[Unit] =
      sql.remove(ids).run.map(_ => ())
  }

  final class OrdersRepoTracing[F[_]: Apply: Logging] extends OrdersRepo[Mid[F, *]] {

    def getBuyWall(pairId: PairId, limit: Long): Mid[F, List[Bid]] =
      _ <* trace"getBuyWall(pairId=$pairId,limit=$limit)"

    def getSellWall(pairId: PairId, limit: Long): Mid[F, List[Ask]] =
      _ <* trace"getSellWall(pairId=$pairId,limit=$limit)"

    def insert(orders: NonEmptyList[AnyOrder]): Mid[F, Unit] =
      _ <* trace"insert(orders=$orders)"

    def remove(ids: NonEmptyList[OrderId]): Mid[F, Unit] =
      _ <* trace"remove(ids=$ids)"
  }
}
