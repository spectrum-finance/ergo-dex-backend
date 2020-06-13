package org.ergoplatform.dex.domain.models

import org.ergoplatform.dex.AssetId

/** Global market order.
  */
final case class Order[T <: OrderType](
  `type`: T,
  asset: AssetId,
  baseAsset: AssetId,
  amount: Long,
  price: Long,
  feePerToken: Double,
  meta: OrderMeta
) {
  val fee: Double = feePerToken * amount
  val id: Long = ???
}

object Order {

  type SellOrder = Order[OrderType.Sell.type]
  type BuyOrder  = Order[OrderType.Buy.type]
  type AnyOrder  = Order[_ <: OrderType]

  def mkBuy(
    asset: AssetId,
    baseAsset: AssetId,
    amount: Long,
    price: Long,
    feePerToken: Double,
    meta: OrderMeta
  ): BuyOrder =
    Order(OrderType.Buy, asset, baseAsset, amount, price, feePerToken, meta)

  def mkSell(
    asset: AssetId,
    baseAsset: AssetId,
    amount: Long,
    price: Long,
    feePerToken: Double,
    meta: OrderMeta
  ): SellOrder =
    Order(OrderType.Sell, asset, baseAsset, amount, price, feePerToken, meta)
}
