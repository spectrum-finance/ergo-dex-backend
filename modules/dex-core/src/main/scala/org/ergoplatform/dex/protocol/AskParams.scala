package org.ergoplatform.dex.protocol

import org.ergoplatform.dex.AssetId

final case class AskParams(baseAsset: AssetId, quoteAsset: AssetId, amount: Long, price: Long)
