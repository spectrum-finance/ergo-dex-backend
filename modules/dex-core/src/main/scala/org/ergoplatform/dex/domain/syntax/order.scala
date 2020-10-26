package org.ergoplatform.dex.domain.syntax

import cats.instances.tuple._
import cats.syntax.bifunctor._
import cats.syntax.list._
import io.estatico.newtype.ops._
import org.ergoplatform.dex.domain.OrderComparator
import org.ergoplatform.dex.domain.models.Order.{AnyOrder, Ask, Bid}
import org.ergoplatform.dex.domain.models.Trade.AnyTrade
import org.ergoplatform.dex.domain.models.{Order, OrderType, Trade}
import org.ergoplatform.dex.{OrderId, PairId}

import scala.annotation.tailrec

object order {

  implicit final class OrderOps[T <: OrderType](private val order: Order[T]) extends AnyVal {

    def satisfies[T1 <: OrderType](thatOrder: Order[T1])(implicit
      cmp: OrderComparator[T, T1]
    ): Boolean = cmp.compare(order, thatOrder) >= 0

    def fillWith[T1 <: OrderType](
      counterOrders: List[Order[T1]]
    )(implicit cmp: OrderComparator[T, T1]): Option[AnyTrade] = {
      @tailrec def fill(orders: List[Order[T1]], acc: List[Order[T1]], toFill: Long): List[Order[T1]] =
        orders match {
          case thatOrder :: rem if (toFill > 0) && (order satisfies thatOrder) =>
            fill(rem, thatOrder +: acc, toFill - thatOrder.amount)
          case _ => acc
        }
      fill(counterOrders, Nil, order.amount).toNel.map(Trade(order, _))
    }

    def id: OrderId = order.meta.boxId.value.coerce[OrderId]

    def fee: Long = order.feePerToken * order.amount

    def pairId: PairId = PairId(order.quoteAsset, order.baseAsset)
  }

  implicit final class OrdersOps(private val xs: List[AnyOrder]) extends AnyVal {

    def partitioned: (List[Ask], List[Bid]) =
      xs.partition(_.`type`.isAsk)
        .bimap(_.map(_.asInstanceOf[Ask]), _.map(_.asInstanceOf[Bid]))
  }
}
