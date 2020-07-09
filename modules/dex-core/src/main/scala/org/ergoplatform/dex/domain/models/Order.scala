package org.ergoplatform.dex.domain.models

import org.ergoplatform.dex.AssetId

/** Global market order.
  * @param `type` - type of the order (sell or buy)
  * @param asset  - id of the asset X
  * @param baseAsset - id of the asset Y
  * @param amount - amount of the `asset`
  * @param price - price for `asset` in `baseAsset`
  * @param feePerToken - amount of fee (in nanoERG) per one traded `asset`
  * @param meta - order metadata
  */
final case class Order[T <: OrderType](
  `type`: T,
  asset: AssetId,
  baseAsset: AssetId,
  amount: Long,
  price: Long,
  feePerToken: Long,
  meta: OrderMeta
)

object Order {

  type SellOrder = Order[OrderType.Sell.type]
  type BuyOrder  = Order[OrderType.Buy.type]
  type AnyOrder  = Order[_ <: OrderType]

  def mkBuy(
    asset: AssetId,
    baseAsset: AssetId,
    amount: Long,
    price: Long,
    feePerToken: Long,
    meta: OrderMeta
  ): BuyOrder =
    Order(OrderType.Buy, asset, baseAsset, amount, price, feePerToken, meta)

  def mkSell(
    asset: AssetId,
    baseAsset: AssetId,
    amount: Long,
    price: Long,
    feePerToken: Long,
    meta: OrderMeta
  ): SellOrder =
    Order(OrderType.Sell, asset, baseAsset, amount, price, feePerToken, meta)
}
