package org.ergoplatform.dex.executor.amm.repositories

import cats.{FlatMap, Functor}
import derevo.derive
import org.ergoplatform.common.cache.Cache
import org.ergoplatform.dex.domain.amm.{CFMMOrder, OrderId}
import org.ergoplatform.dex.executor.amm.repositories.CFMMPools.{CFMMPoolsTracing, Live}
import tofu.higherKind.Mid
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._

@derive(representableK)
trait CFMMOrders[F[_]] {

  def put(order: CFMMOrder): F[Unit]

  def exists(orderId: OrderId): F[Boolean]

  def drop(orderId: OrderId): F[Unit]

  def get(orderId: OrderId): F[Option[CFMMOrder]]

  def getAll: F[List[CFMMOrder]]
}

object CFMMOrders {

  def make[I[_]: Functor, F[_]: FlatMap](implicit logs: Logs[I, F], cache: Cache[F]): I[CFMMOrders[F]] =
    logs.forService[CFMMOrders[F]].map { implicit logging =>
      new CFMMOrdersTracingMid[F] attach new Live[F](cache)
    }

  final private class Live[F[_]](cache: Cache[F]) extends CFMMOrders[F] {

    def put(order: CFMMOrder): F[Unit] = cache.set(order.id, order)

    def exists(orderId: OrderId): F[Boolean] = cache.exists(orderId)

    def drop(orderId: OrderId): F[Unit] = cache.del(orderId)

    def get(orderId: OrderId): F[Option[CFMMOrder]] = cache.get[OrderId, CFMMOrder](orderId)

    def getAll: F[List[CFMMOrder]] = cache.getAll
  }

  final private class CFMMOrdersTracingMid[F[_]: FlatMap: Logging] extends CFMMOrders[Mid[F, *]] {

    def put(order: CFMMOrder): Mid[F, Unit] = for {
      _ <- trace"put(order=$order)"
      r <- _
      _ <- trace"put(order=$order) -> $r"
    } yield r

    def exists(orderId: OrderId): Mid[F, Boolean] = for {
      _ <- trace"exists(orderId=$orderId)"
      r <- _
      _ <- trace"exists(orderId=$orderId) -> $r"
    } yield r

    def drop(orderId: OrderId): Mid[F, Unit] = for {
      _ <- trace"drop(orderId=$orderId)"
      r <- _
      _ <- trace"drop(orderId=$orderId) -> $r"
    } yield r

    def get(orderId: OrderId): Mid[F, Option[CFMMOrder]] = for {
      _ <- trace"checkLater(order=$orderId)"
      r <- _
      _ <- trace"checkLater(order=$orderId) -> $r"
    } yield r

    def getAll: Mid[F, List[CFMMOrder]] = for {
      _ <- trace"getAll()"
      r <- _
      _ <- trace"getAll() -> length: ${r.length}"
    } yield r
  }
}
