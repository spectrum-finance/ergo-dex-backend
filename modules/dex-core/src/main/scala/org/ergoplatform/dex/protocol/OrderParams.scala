package org.ergoplatform.dex.protocol

import org.ergoplatform.dex.domain.models.OrderType
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
