package org.ergoplatform.dex.executor.amm.generators

import cats.effect.Sync
import org.ergoplatform.dex.domain.amm
import org.ergoplatform.dex.domain.amm.CFMMOrder
import org.ergoplatform.dex.executor.amm.repositories.CFMMOrders
import tofu.concurrent.MakeAtom
import tofu.syntax.monadic._

object CFMMOrdersGenerator {

  def genMapBased[I[_]: Sync, F[_]: Sync]: I[CFMMOrders[F]] = for {
    map <- MakeAtom[I, F].of(Map.empty[amm.OrderId, CFMMOrder])
  } yield (new CFMMOrders[F] {

    override def put(order: CFMMOrder): F[Unit] =
      map.update(_ + (order.id -> order))

    override def exists(orderId: amm.OrderId): F[Boolean] =
      map.get.map(_.exists(_._1 == orderId))

    override def drop(orderId: amm.OrderId): F[Unit] =
      map.update(_.filter(_._1 != orderId))

    override def get(orderId: amm.OrderId): F[Option[CFMMOrder]] =
      map.get.map(_.get(orderId))

    override def getAll: F[List[CFMMOrder]] =
      map.get.map(_.values.toList)
  })
}
