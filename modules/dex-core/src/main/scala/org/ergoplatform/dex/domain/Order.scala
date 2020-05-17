package org.ergoplatform.dex.domain

import org.ergoplatform.dex.AssetId

final case class Order(
  sellAsset: AssetId,
  buyAsset: AssetId,
  amount: Long,
  price: Long,
  meta: OrderMeta
)
