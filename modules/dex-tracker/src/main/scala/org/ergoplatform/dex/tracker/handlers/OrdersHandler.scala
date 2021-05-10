package org.ergoplatform.dex.tracker.handlers

import cats.syntax.option.none
import cats.{Defer, Functor, FunctorFilter, Monad, MonoidK}
import mouse.any._
import org.ergoplatform.dex.domain.orderbook.Order.AnyOrder
import org.ergoplatform.dex.domain.orderbook.OrderId
import org.ergoplatform.dex.protocol.orderbook.OrderContractFamily
import org.ergoplatform.dex.streaming.{Producer, Record}
import org.ergoplatform.dex.tracker.domain.errors.InvalidOrder
import org.ergoplatform.dex.tracker.parsers.orders.OrdersOps
import tofu.logging.{Logging, Logs}
import tofu.streams.{Evals, Pace}
import tofu.syntax.handle._
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.streams.all._
import tofu.{Catches, Handle}

final class OrdersHandler[
  CT <: OrderContractFamily,
  F[_]: Monad: Evals[*[_], G]: Pace: FunctorFilter: Defer: MonoidK: Catches,
  G[_]: Monad: Handle[*[_], InvalidOrder]: Logging
](implicit producer: Producer[OrderId, AnyOrder, F], orders: OrdersOps[CT, G]) {

  def handler: BoxHandler[F] =
    _.evalMap { out =>
      orders
        .parseOrder(out)
        .handleWith[InvalidOrder] { e =>
          warnCause"Skipping invalid order in box $out" (e) as none[AnyOrder]
        }
    }.unNone
      .evalTap(order => info"Order detected $order")
      .map(o => Record[OrderId, AnyOrder](o.id, o))
      .thrush(producer.produce)
}

object OrdersHandler {

  def make[
    CT <: OrderContractFamily,
    I[_]: Functor,
    F[_]: Monad: Evals[*[_], G]: Pace: FunctorFilter: Defer: MonoidK: Catches,
    G[_]: Monad: Handle[*[_], InvalidOrder]
  ](implicit producer: Producer[OrderId, AnyOrder, F], orders: OrdersOps[CT, G], logs: Logs[I, G]): I[BoxHandler[F]] =
    logs.forService[OrdersHandler[CT, F, G]].map(implicit log => new OrdersHandler[CT, F, G].handler)
}
