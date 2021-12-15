package org.ergoplatform.dex.domain

import org.ergoplatform.ergo.TokenId

final case class AssetClass(tokenId: TokenId, ticker: Option[Ticker], decimals: Option[Int]) {
  def assetInstance(amount: Long): FullAsset = FullAsset(tokenId, amount, ticker, decimals)
}
