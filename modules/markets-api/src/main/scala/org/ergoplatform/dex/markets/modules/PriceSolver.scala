package org.ergoplatform.dex.markets.modules

import org.ergoplatform.dex.domain.{AssetEquiv, FullAsset, ValueUnits}

trait PriceSolver[F[_]] {

  def solve(asset: FullAsset, targetUnits: ValueUnits): F[AssetEquiv]
}
