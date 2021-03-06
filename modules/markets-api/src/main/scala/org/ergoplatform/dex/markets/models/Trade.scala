package org.ergoplatform.dex.markets.models

import derevo.derive
import org.ergoplatform.dex.{AssetId, TxId}
import tofu.logging.derivation.loggable

@derive(loggable)
final case class Trade(
  side: Side,
  txId: TxId,
  height: Int,
  quoteAsset: AssetId,
  baseAsset: AssetId,
  amount: Long,
  price: Long,
  fee: Long,
  ts: Long
)
