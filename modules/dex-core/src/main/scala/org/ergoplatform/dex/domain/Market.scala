package org.ergoplatform.dex.domain

import org.ergoplatform.ergo.TokenId

final case class Market(x: AssetClass, y: AssetClass, price: Price) {
  def contains(asset: TokenId): Boolean = x.tokenId == asset || y.tokenId == asset

  def priceBy(tokenId: TokenId): BigDecimal =
    if (tokenId == x.tokenId) price.byX
    else price.byY
}
