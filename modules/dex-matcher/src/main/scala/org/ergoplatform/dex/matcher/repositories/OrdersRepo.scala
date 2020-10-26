package org.ergoplatform.dex.matcher.repositories

import cats.data.NonEmptyList
import derevo.derive
import doobie.ConnectionIO
import doobie.util.log.LogHandler
import org.ergoplatform.dex.{OrderId, PairId}
import org.ergoplatform.dex.domain.models.Order.{AnyOrder, Ask, Bid}
import tofu.higherKind.derived.representableK

@derive(representableK)
trait OrdersRepo[D[_]] {

  def getBuyWall(pairId: PairId, limit: Long): D[List[Bid]]

  def getSellWall(pairId: PairId, limit: Long): D[List[Ask]]

  def insert(orders: NonEmptyList[AnyOrder]): D[Unit]

  def remove(ids: NonEmptyList[OrderId]): D[Unit]
}

object OrdersRepo {

  final class Live(implicit lh: LogHandler) extends OrdersRepo[ConnectionIO] {

    import org.ergoplatform.dex.matcher.sql.{ordersSql => sql}

    def getBuyWall(pairId: PairId, limit: Long): ConnectionIO[List[Bid]] =
      sql.getBuyWall(pairId).stream.take(limit).compile.toList

    def getSellWall(pairId: PairId, limit: Long): ConnectionIO[List[Ask]] =
      sql.getSellWall(pairId).stream.take(limit).compile.toList

    def insert(orders: NonEmptyList[AnyOrder]): ConnectionIO[Unit] =
      sql.insert.updateManyWithGeneratedKeys(sql.fields: _*)(orders).compile.drain

    def remove(ids: NonEmptyList[OrderId]): ConnectionIO[Unit] =
      sql.remove(ids).run.map(_ => ())
  }
}
