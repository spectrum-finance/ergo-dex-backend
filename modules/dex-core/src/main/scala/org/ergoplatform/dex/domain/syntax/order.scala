package org.ergoplatform.dex.domain.syntax

import cats.syntax.bifunctor._
import cats.instances.tuple._
import cats.syntax.list._
import org.ergoplatform.dex.PairId
import org.ergoplatform.dex.domain.OrderComparator
import org.ergoplatform.dex.domain.models.Match.AnyMatch
import org.ergoplatform.dex.domain.models.Order.{AnyOrder, BuyOrder, SellOrder}
import org.ergoplatform.dex.domain.models.{Match, Order, OrderType}

import scala.annotation.tailrec

object order {

  implicit final class OrderOps[T <: OrderType](private val order: Order[T]) extends AnyVal {

    def satisfies[T1 <: OrderType](thatOrder: Order[T1])(
      implicit cmp: OrderComparator[T, T1]
    ): Boolean = cmp.compare(order, thatOrder) >= 0

    def fillWith[T1 <: OrderType](
      counterOrders: List[Order[T1]]
    )(implicit cmp: OrderComparator[T, T1]): Option[AnyMatch] = {
      @tailrec def fill(orders: List[Order[T1]], acc: List[Order[T1]], toFill: Long): List[Order[T1]] =
        orders match {
          case thatOrder :: rem if (toFill > 0) && (order satisfies thatOrder) =>
            fill(rem, thatOrder +: acc, toFill - thatOrder.amount)
          case _ => acc
        }
      fill(counterOrders, Nil, order.amount).toNel.map(Match(order, _))
    }

    def pairId: PairId = ???
  }

  implicit final class OrdersOps(private val xs: List[AnyOrder]) extends AnyVal {

    def partitioned: (List[SellOrder], List[BuyOrder]) =
      xs.partition(_.`type`.isSell)
        .bimap(_.map(_.asInstanceOf[SellOrder]), _.map(_.asInstanceOf[BuyOrder]))
  }
}
