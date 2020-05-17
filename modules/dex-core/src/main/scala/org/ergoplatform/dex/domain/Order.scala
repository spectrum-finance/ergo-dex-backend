package org.ergoplatform.dex.domain

import org.ergoplatform.dex.AssetId

/** Global market order.
  */
final case class Order(
  sellAsset: AssetId,
  buyAsset: AssetId,
  amount: Long,
  price: Long,
  feePerToken: Double,
  meta: OrderMeta
)
