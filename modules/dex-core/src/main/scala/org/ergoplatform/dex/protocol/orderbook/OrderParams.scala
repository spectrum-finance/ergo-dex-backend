package org.ergoplatform.dex.protocol.orderbook

import org.ergoplatform.dex.domain.orderbook.OrderType
import org.ergoplatform.dex.{AssetId, SErgoTree}

final case class OrderParams(
  orderType: OrderType,
  baseAsset: AssetId,
  quoteAsset: AssetId,
  amount: Long,
  price: Long,
  feePerToken: Long,
  ownerErgoTree: SErgoTree
)
