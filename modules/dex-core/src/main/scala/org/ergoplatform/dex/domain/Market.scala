package org.ergoplatform.dex.domain

import org.ergoplatform.ergo.TokenId

final case class Market(x: AssetClass, y: AssetClass, price: Price) {
  def contains(asset: TokenId): Boolean = x.tokenId == asset || y.tokenId == asset

  def priceBy(tokenId: TokenId): BigDecimal =
    if (tokenId == x.tokenId) price.byX
    else price.byY
}

object Market {

  def fromReserves(rx: FullAsset, ry: FullAsset): Market =
    Market(rx.assetClass, ry.assetClass, Price(BigDecimal(ry.amount) / rx.amount, BigDecimal(rx.amount) / ry.amount))
}
