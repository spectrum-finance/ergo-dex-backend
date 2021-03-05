package org.ergoplatform.dex.protocol

import org.ergoplatform.dex.AssetId

final case class BidParams(baseAsset: AssetId, quoteAsset: AssetId, amount: Long, price: Long)
