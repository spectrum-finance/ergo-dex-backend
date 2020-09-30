package org.ergoplatform.dex.domain.models

import org.ergoplatform.dex.AssetId

/** Global market order.
  * @param `type` - type of the order (sell or buy)
  * @param quoteAsset  - id of the quote asset
  * @param baseAsset - id of the base asset
  * @param amount - amount of `asset`
  * @param price - price for `asset` in `baseAsset`
  * @param feePerToken - amount of fee (in nanoERG) per one traded `asset`
  * @param meta - order metadata
  */
final case class Order[T <: OrderType](
  `type`: T,
  quoteAsset: AssetId,
  baseAsset: AssetId,
  amount: Long,
  price: Long,
  feePerToken: Long,
  meta: OrderMeta
)

object Order {

  type Ask      = Order[OrderType.Ask.type]
  type Bid      = Order[OrderType.Bid.type]
  type AnyOrder = Order[_ <: OrderType]

  def mkBid(
    quoteAsset: AssetId,
    baseAsset: AssetId,
    amount: Long,
    price: Long,
    feePerToken: Long,
    meta: OrderMeta
  ): Bid =
    Order(OrderType.Bid, quoteAsset, baseAsset, amount, price, feePerToken, meta)

  def mkAsk(
    quoteAsset: AssetId,
    baseAsset: AssetId,
    amount: Long,
    price: Long,
    feePerToken: Long,
    meta: OrderMeta
  ): Ask =
    Order(OrderType.Ask, quoteAsset, baseAsset, amount, price, feePerToken, meta)
}
