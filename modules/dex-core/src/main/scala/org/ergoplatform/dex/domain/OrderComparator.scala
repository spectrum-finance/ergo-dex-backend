package org.ergoplatform.dex.domain

import org.ergoplatform.dex.domain.models.OrderType.{Buy, Sell}
import org.ergoplatform.dex.domain.models.{Order, OrderType}

trait OrderComparator[T0 <: OrderType, T1 <: OrderType] {
  def compare(x: Order[T0], y: Order[T1]): Int
}

object OrderComparator {

  implicit val sellBuyComparator: OrderComparator[OrderType.Sell, OrderType.Buy] =
    (sell: Order[Sell], buy: Order[Buy]) =>
      if (sell.price < buy.price) 1
      else if (sell.price > buy.price) -1
      else 0

  implicit val buySellComparator: OrderComparator[OrderType.Buy, OrderType.Sell] =
    (buy: Order[Buy], sell: Order[Sell]) =>
      if (sell.price < buy.price) 1
      else if (sell.price > buy.price) -1
      else 0
}
