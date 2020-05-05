package org.ergoplatform.dex.domain

import org.ergoplatform.dex.TokenId

final case class Order[OT <: OrderType](
  `type`: OT,
  pair: (TokenId, TokenId),
  amount: Long,
  limit: Long
)

object Order {

  type SellOrder = Order[OrderType.Sell.type]
  type BuyOrder  = Order[OrderType.Buy.type]
  type AnyOrder  = Order[_ <: OrderType]

  def mkBuy(pair: (TokenId, TokenId), amount: Long, limit: Long): BuyOrder =
    Order(OrderType.Buy, pair, amount, limit)

  def mkSell(pair: (TokenId, TokenId), amount: Long, limit: Long): SellOrder =
    Order(OrderType.Sell, pair, amount, limit)
}
