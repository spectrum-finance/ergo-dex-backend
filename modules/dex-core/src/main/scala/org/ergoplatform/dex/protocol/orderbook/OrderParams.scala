package org.ergoplatform.dex.protocol.orderbook

import org.ergoplatform.dex.domain.orderbook.OrderType
import org.ergoplatform.dex.{TokenId, SErgoTree}

final case class OrderParams(
                              orderType: OrderType,
                              baseAsset: TokenId,
                              quoteAsset: TokenId,
                              amount: Long,
                              price: Long,
                              feePerToken: Long,
                              ownerErgoTree: SErgoTree
)
