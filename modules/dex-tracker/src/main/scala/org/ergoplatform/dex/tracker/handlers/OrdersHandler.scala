package org.ergoplatform.dex.tracker.handlers

import cats.syntax.option.none
import cats.{Defer, Functor, FunctorFilter, Monad, MonoidK}
import mouse.any._
import org.ergoplatform.dex.OrderId
import org.ergoplatform.dex.domain.orderbook.Order.AnyOrder
import org.ergoplatform.dex.streaming.{Producer, Record}
import org.ergoplatform.dex.tracker.domain.errors.InvalidOrder
import org.ergoplatform.dex.tracker.modules.Orders
import tofu.logging.{Logging, Logs}
import tofu.streams.{Evals, Pace}
import tofu.syntax.handle._
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.streams.all._
import tofu.{Catches, Handle}

final class OrdersHandler[
  F[_]: Monad: Evals[*[_], G]: Pace: FunctorFilter: Defer: MonoidK: Catches,
  G[_]: Monad: Handle[*[_], InvalidOrder]: Logging
](implicit producer: Producer[OrderId, AnyOrder, F], orders: Orders[G]) {

  def handler: BoxHandler[F] =
    _.evalMap { out =>
      orders
        .makeOrder(out)
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
    I[_]: Functor,
    F[_]: Monad: Evals[*[_], G]: Pace: FunctorFilter: Defer: MonoidK: Catches,
    G[_]: Monad: Handle[*[_], InvalidOrder]
  ](implicit producer: Producer[OrderId, AnyOrder, F], orders: Orders[G], logs: Logs[I, G]): I[BoxHandler[F]] =
    logs.forService[OrdersHandler[F, G]].map(implicit log => new OrdersHandler[F, G].handler)
}
