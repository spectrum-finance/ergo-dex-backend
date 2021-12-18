package org.ergoplatform.dex.domain

import cats.Show
import tofu.logging.Loggable

final case class AssetEquiv[T](asset: FullAsset, units: ValueUnits[T], value: BigDecimal)

object AssetEquiv {

  implicit def show[T]: Show[AssetEquiv[T]] =
    Show.show(e => s"AssetEquiv{assetId=${e.asset.id}, units=${e.units}, value=${e.value}")

  implicit def loggable[T]: Loggable[AssetEquiv[T]] = Loggable.show
}
