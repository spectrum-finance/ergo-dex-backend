package org.ergoplatform.dex.domain

import derevo.derive
import org.ergoplatform.ergo.TokenId
import tofu.logging.derivation.loggable

@derive(loggable)
final case class Market(x: AssetClass, y: AssetClass, price: Price) {
  def contains(asset: TokenId): Boolean = x.tokenId == asset || y.tokenId == asset

  def priceBy(tokenId: TokenId): BigDecimal =
    if (tokenId == x.tokenId) price.byX
    else price.byY
}

object Market {

  val Base10 = BigDecimal(10)

  def fromReserves(rx: FullAsset, ry: FullAsset): Market =
    Market(
      rx.assetClass,
      ry.assetClass,
      Price(
        BigDecimal(ry.amount) / rx.amount * Base10.pow(rx.evalDecimals - ry.evalDecimals),
        BigDecimal(rx.amount) / ry.amount * Base10.pow(ry.evalDecimals - rx.evalDecimals)
      )
    )
}
