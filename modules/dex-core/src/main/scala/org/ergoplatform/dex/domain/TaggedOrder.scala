package org.ergoplatform.dex.domain

sealed trait TaggedOrder {
  val order: Order
}

object TaggedOrder {
  final case class SellOrder(order: Order) extends TaggedOrder
  final case class BuyOrder(order: Order) extends TaggedOrder
}
