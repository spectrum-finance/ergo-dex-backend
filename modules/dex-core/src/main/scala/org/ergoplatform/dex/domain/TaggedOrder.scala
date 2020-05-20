package org.ergoplatform.dex.domain

sealed trait TaggedOrder {
  val order: Order
  def isSell: Boolean = false
}

object TaggedOrder {

  final case class SellOrder(order: Order) extends TaggedOrder {
    override def isSell: Boolean = true
  }
  final case class BuyOrder(order: Order) extends TaggedOrder
}
