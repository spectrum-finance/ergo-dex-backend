package org.ergoplatform.dex.executor.amm.services

import cats.effect.{Clock, Sync}
import cats.{FlatMap, Monad}
import derevo.derive
import org.ergoplatform.dex.domain.amm.{CFMMOrder, OrderId, WeightedOrder}
import org.ergoplatform.dex.executor.amm.repositories.CFMMOrders
import tofu.concurrent.{Atom, MakeAtom}
import cats.syntax.traverse._
import cats.syntax.option._
import org.ergoplatform.common.cache.CacheStreaming
import org.ergoplatform.dex.executor.amm.config.BacklogConfig
import tofu.concurrent.MakeAtom._
import tofu.generate.GenRandom
import tofu.higherKind.Mid
import tofu.higherKind.derived.representableK
import tofu.lift.IsoK
import tofu.logging.{Logging, Logs}
import tofu.streams.{Compile, Evals}
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.streams.compile._
import tofu.syntax.embed._
import tofu.syntax.time.now
import tofu.syntax.streams.evals._

import scala.collection.mutable

@derive(representableK)
trait CFMMBacklog[F[_]] {

  /** Put an order to the backlog.
    */
  def put(order: CFMMOrder): F[Unit]

  /** Put an order with priceTooHigh or priceTooLow execution err
    */
  def suspend(order: CFMMOrder): F[Unit]

  /** Put possibly executed order to the backlog
    */
  def checkLater(order: CFMMOrder): F[Unit]

  /** Get candidate order for execution. Blocks until an order is available.
    */
  def get: F[Option[CFMMOrder]]

  /** Put an order from the backlog.
    */
  def drop(id: OrderId): F[Unit]
}

object CFMMBacklog {

  def make[
    I[_]: Sync,
    F[_]: Monad: Evals[*[_], G]: Compile[*[_], G],
    G[_]: Sync: BacklogConfig.Has: Clock: GenRandom
  ](implicit
    logs: Logs[I, G],
    cfmmOrders: CFMMOrders[G],
    isoKGI: IsoK[G, I],
    streamingCache: CacheStreaming[F]
  ): I[CFMMBacklog[G]] = for {
    implicit0(logging: Logging[G]) <- logs.forService[CFMMBacklog[G]]
    pendingQueueRef                <- MakeAtom[I, G].of(mutable.PriorityQueue.empty[WeightedOrder])
    suspendedQueueRef              <- MakeAtom[I, G].of(mutable.PriorityQueue.empty[WeightedOrder])
    revisitOrders                  <- MakeAtom[I, G].of(List.empty[WeightedOrder])
    _                              <- isoKGI.to(recoverPendingQueue[F, G](pendingQueueRef, streamingCache).drain)
  } yield BacklogConfig.access
    .map(cfg =>
      new CFMMBacklogTracingMid[G] attach new Live[G](
        pendingQueueRef,
        suspendedQueueRef,
        revisitOrders,
        cfmmOrders,
        cfg
      )
    )
    .embed

  final private class Live[F[_]: Monad: Clock: GenRandom](
    pendingQueueRef: Atom[F, mutable.PriorityQueue[WeightedOrder]],
    suspendedQueueRef: Atom[F, mutable.PriorityQueue[WeightedOrder]],
    revisitOrders: Atom[F, List[WeightedOrder]],
    cfmmOrders: CFMMOrders[F],
    backlogConfig: BacklogConfig
  ) extends CFMMBacklog[F] {

    /** Put an order to the backlog.
      */
    def put(order: CFMMOrder): F[Unit] =
      cfmmOrders.put(order) >> pendingQueueRef.update(_ += WeightedOrder.fromOrder(order))

    /** Put an order with priceTooHigh or priceTooLow execution err
      */
    def suspend(order: CFMMOrder): F[Unit] =
      suspendedQueueRef.update(_ += WeightedOrder.fromOrder(order))

    /** Put possibly executed order to the backlog
      */
    def checkLater(order: CFMMOrder): F[Unit] =
      revisitOrders.update(_ :+ WeightedOrder.fromOrder(order))

    /** Get candidate order for execution. Blocks until an order is available.
      */
    def get: F[Option[CFMMOrder]] = for {
      _      <- filterRevisit
      random <- GenRandom.nextInt[F](100)
      order <-
        if (random > backlogConfig.suspendedOrdersExecutionProbabilityPercent)
          getMaxOrderFromQueue(pendingQueueRef)
        else
          getMaxOrderFromQueue(suspendedQueueRef)
    } yield order

    /** Put an order from the backlog.
      */
    def drop(id: OrderId): F[Unit] =
      cfmmOrders.drop(id)

    def getMaxOrderFromQueue(queue: Atom[F, mutable.PriorityQueue[WeightedOrder]]): F[Option[CFMMOrder]] =
      for {
        maxId <- queue.modify { queue =>
                   val elem = if (queue.isEmpty) none else queue.dequeue().some
                   (queue, elem)
                 }
        time <- now.millis
        suspendedElem <- maxId match {
                           case Some(value) if (time - value.timestamp) < backlogConfig.orderLifetime.toMillis =>
                             cfmmOrders.get(value.orderId) >>= {
                               case Some(value) => value.some.pure[F]
                               case None        => getMaxOrderFromQueue(queue)
                             }
                           case Some(value) =>
                             cfmmOrders.drop(value.orderId) >> getMaxOrderFromQueue(queue)
                           case None => none.pure[F]
                         }
      } yield suspendedElem

    def filterRevisit: F[Unit] = for {
      curTime <- now.millis
      possible2pending <-
        revisitOrders.modify(_.span(wOrd => (curTime - wOrd.timestamp < backlogConfig.orderExecutionTime.toMillis)))
      _ <- possible2pending.traverse {
             case ord if (curTime - ord.timestamp) > backlogConfig.orderLifetime.toMillis =>
               drop(ord.orderId)
             case ord =>
               pendingQueueRef.update(_ += ord)
           }
    } yield ()
  }

  private def recoverPendingQueue[F[_]: Evals[*[_], G]: Monad, G[_]: Monad: Clock: BacklogConfig.Has](
    pendingQueue: Atom[G, mutable.PriorityQueue[WeightedOrder]],
    cfmmOrders: CacheStreaming[F]
  ): F[Unit] = for {
    cfg           <- eval(BacklogConfig.access)
    order         <- cfmmOrders.getAll[CFMMOrder]
    curTime       <- eval(now.millis)
    _             <- eval(
      if (curTime - order.timestamp < cfg.orderLifetime.toMillis)
        pendingQueue.update(_ += WeightedOrder.fromOrder(order))
      else
        ().pure[G]
    )
  } yield ()

  final private class CFMMBacklogTracingMid[F[_]: FlatMap: Logging] extends CFMMBacklog[Mid[F, *]] {

    /** Put an order to the backlog.
      */
    override def put(order: CFMMOrder): Mid[F, Unit] = for {
      _ <- trace"put(order=$order)"
      r <- _
      _ <- trace"put(order=$order) -> $r"
    } yield r

    /** Put an order with priceTooHigh or priceTooLow execution err
      */
    override def suspend(order: CFMMOrder): Mid[F, Unit] = for {
      _ <- trace"suspend(order=$order)"
      r <- _
      _ <- trace"suspend(order=$order) -> $r"
    } yield r

    /** Put possibly executed order to the backlog
      */
    override def checkLater(order: CFMMOrder): Mid[F, Unit] = for {
      _ <- trace"checkLater(order=$order)"
      r <- _
      _ <- trace"checkLater(order=$order) -> $r"
    } yield r

    /** Get candidate order for execution. Blocks until an order is available.
      */
    override def get: Mid[F, Option[CFMMOrder]] = for {
      _ <- trace"get()"
      r <- _
      _ <- trace"get() -> $r"
    } yield r

    /** Put an order from the backlog.
      */
    override def drop(id: OrderId): Mid[F, Unit] = for {
      _ <- trace"drop(id=$id)"
      r <- _
      _ <- trace"drop(id=$id) -> $r"
    } yield r
  }
}
