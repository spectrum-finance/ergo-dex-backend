package org.ergoplatform.dex.markets.models

import org.ergoplatform.dex.{AssetId, TxId}

final case class Trade(
  txId: TxId,
  height: Int,
  quoteAsset: AssetId,
  baseAsset: AssetId,
  amount: Long,
  price: Long,
  fee: Long,
  ts: Long
)
