package org.ergoplatform.dex.domain

final case class AssetEquiv[T](asset: FullAsset, units: ValueUnits[T], value: BigDecimal)
