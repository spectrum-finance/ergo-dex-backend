package org.ergoplatform.dex.markets.api.v1.models.orderbook

import derevo.derive
import org.ergoplatform.ergo.{TokenId, TxId}
import tofu.logging.derivation.loggable

@derive(loggable)
final case class Fill(
  side: Side,
  txId: TxId,
  quoteAsset: TokenId,
  baseAsset: TokenId,
  amount: Long,
  price: Long,
  fee: Long
)
