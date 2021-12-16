package org.ergoplatform.dex.domain

import org.ergoplatform.ergo.TokenId

final case class FullAsset(
  id: TokenId,
  amount: Long,
  ticker: Option[Ticker],
  decimals: Option[Int]
) {
  def assetClass: AssetClass = AssetClass(id, ticker, decimals)
}
