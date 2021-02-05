package org.ergoplatform.dex.domain

import org.ergoplatform.dex.domain.models.OrderType.{Bid, Ask}
import org.ergoplatform.dex.domain.models.{Order, OrderType}

trait OrderComparator[T0 <: OrderType, T1 <: OrderType] {
  def compare(x: Order[T0], y: Order[T1]): Int
}

object OrderComparator {

  implicit val sellBuyComparator: OrderComparator[OrderType.Ask, OrderType.Bid] =
    (sell: Order[Ask], buy: Order[Bid]) =>
      if (sell.price < buy.price) 1
      else if (sell.price > buy.price) -1
      else 0

  implicit val buySellComparator: OrderComparator[OrderType.Bid, OrderType.Ask] =
    (buy: Order[Bid], sell: Order[Ask]) =>
      if (sell.price < buy.price) 1
      else if (sell.price > buy.price) -1
      else 0
}
