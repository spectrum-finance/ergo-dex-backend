package org.ergoplatform.dex.executor.amm.services

import cats.effect.IO
import cats.syntax.traverse._
import org.ergoplatform.dex.domain.amm.{CFMMOrder, WeightedOrder}
import org.scalatest.flatspec.AnyFlatSpec
import org.ergoplatform.dex.executor.amm._
import org.scalatest.matchers.should
import org.ergoplatform.dex.executor.amm.generators._
import org.ergoplatform.dex.executor.amm.utils.isoK._
import org.ergoplatform.dex.executor.amm.repositories.CFMMOrders
import org.ergoplatform.dex.executor.amm.utils.Ordering.checkDescSort
import org.ergoplatform.dex.executor.amm.utils.genRandoms.genRandom
import tofu.logging.Logs
import org.ergoplatform.dex.executor.amm.services.StreamF
import tofu.fs2Instances._
import fs2.Stream
import org.ergoplatform.common.cache.CacheStreaming

import scala.concurrent.ExecutionContext

class CFMMBacklogTests extends AnyFlatSpec with should.Matchers {

  implicit val timer = IO.timer(ExecutionContext.global)

  implicit val logs = Logs.empty[IO, IO]

  "CFMMBacklog" should "correctly retry non executed orders" in {

    val ordersQty = 100

    implicit val cfgHas = configs.has.cfgWithOnlySuspended

    val ordersFromBacklog = for {
      implicit0(cfmmOrders: CFMMOrders[IO]) <- CFMMOrdersGenerator.genMapBased[IO, IO]
      orders                                <- OrdersGenerator.genSwapOrders[IO](ordersQty)
      suspendedOrders =
        orders.map(order =>
          order.copy(timestamp = order.timestamp - configs.backlogCfgWithOnlySuspended.orderExecutionTime.toMillis * 2)
        )
      implicit0(streamCaching: CacheStreaming[StreamF]) = CacheStreamingGenerator.cacheStreamingFor(List.empty)
      backlog     <- CFMMBacklog.make[IO, StreamF, IO]
      _           <- suspendedOrders.traverse(cfmmOrders.put)
      _           <- suspendedOrders.traverse(backlog.suspend)
      fromBacklog <- suspendedOrders.traverse(_ => backlog.get)
    } yield fromBacklog

    val ordersOpts = ordersFromBacklog.unsafeRunSync()

    val orders = ordersOpts.collect[CFMMOrder, List[CFMMOrder]] { case Some(order) =>
      order
    }

    orders.length shouldBe ordersQty

    checkDescSort(orders.map(WeightedOrder.fromOrder)) shouldBe true
  }

  "CFMMBacklog" should "correctly drop outdated orders" in {

    val normalOrdersQty = 50

    val outdatedOrdersQty = 50

    implicit val cfgHas = configs.has.cfgWithNoSuspended

    val ordersFromBacklog = for {
      implicit0(cfmmOrders: CFMMOrders[IO]) <- CFMMOrdersGenerator.genMapBased[IO, IO]
      orders                                <- OrdersGenerator.genSwapOrders[IO](normalOrdersQty + outdatedOrdersQty)
      outdatedOrders =
        orders
          .take(outdatedOrdersQty)
          .map(order =>
            order.copy(timestamp = order.timestamp - configs.backlogCfgWithNoSuspended.orderLifetime.toMillis * 4)
          )
      implicit0(streamCaching: CacheStreaming[StreamF]) = CacheStreamingGenerator.cacheStreamingFor(List.empty)
      normalOrders                                      = orders.drop(outdatedOrdersQty)
      backlog     <- CFMMBacklog.make[IO, StreamF, IO]
      _           <- (normalOrders ++ outdatedOrders).traverse(backlog.put)
      fromBacklog <- normalOrders.traverse(_ => backlog.get)
    } yield fromBacklog

    val ordersOpts = ordersFromBacklog.unsafeRunSync()

    val orders = ordersOpts.collect[CFMMOrder, List[CFMMOrder]] { case Some(order) =>
      order
    }

    orders.length shouldBe normalOrdersQty

    checkDescSort(orders.map(WeightedOrder.fromOrder)) shouldBe true
  }

  "CFMMBacklog" should "correctly process orders by weight during pipeline" in {
    val ordersQty = 100

    implicit val cfgHas = configs.has.cfgWithNoSuspended

    val ordersFromBacklog = for {
      implicit0(cfmmOrders: CFMMOrders[IO]) <- CFMMOrdersGenerator.genMapBased[IO, IO]
      orders                                <- OrdersGenerator.genSwapOrders[IO](ordersQty)
      implicit0(streamCaching: CacheStreaming[StreamF]) = CacheStreamingGenerator.cacheStreamingFor(List.empty)
      backlog     <- CFMMBacklog.make[IO, StreamF, IO]
      _           <- orders.traverse(backlog.put)
      fromBacklog <- orders.traverse(_ => backlog.get)
    } yield fromBacklog

    val ordersOpts = ordersFromBacklog.unsafeRunSync()

    val orders = ordersOpts.collect[CFMMOrder, List[CFMMOrder]] { case Some(order) =>
      order
    }

    orders.length shouldBe ordersQty

    checkDescSort(orders.map(WeightedOrder.fromOrder)) shouldBe true
  }

  "CFMMBacklog" should "correctly restore" in {

    val ordersQty = 100

    implicit val cfgHas = configs.has.cfgWithNoSuspended

    val ordersFromBacklog = for {
      implicit0(cfmmOrders: CFMMOrders[IO]) <- CFMMOrdersGenerator.genMapBased[IO, IO]
      orders                                <- OrdersGenerator.genSwapOrders[IO](ordersQty)
      implicit0(streamCaching: CacheStreaming[StreamF]) = CacheStreamingGenerator.cacheStreamingFor(orders)
      _           <- orders.traverse(cfmmOrders.put)
      backlog     <- CFMMBacklog.make[IO, StreamF, IO]
      fromBacklog <- orders.traverse(_ => backlog.get)
    } yield fromBacklog

    val ordersOpts = ordersFromBacklog.unsafeRunSync()

    val orders = ordersOpts.collect[CFMMOrder, List[CFMMOrder]] { case Some(order) =>
      order
    }

    orders.length shouldBe ordersQty

    checkDescSort(orders.map(WeightedOrder.fromOrder)) shouldBe true
  }
}
