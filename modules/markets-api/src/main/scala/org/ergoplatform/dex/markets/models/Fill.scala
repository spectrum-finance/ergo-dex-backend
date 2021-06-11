package org.ergoplatform.dex.markets.models

import derevo.derive
import org.ergoplatform.ergo.{TokenId, TxId}
import tofu.logging.derivation.loggable

@derive(loggable)
final case class Fill(
  side: Side,
  txId: TxId,
  height: Int,
  quoteAsset: TokenId,
  baseAsset: TokenId,
  amount: Long,
  price: Long,
  fee: Long,
  ts: Long
)
