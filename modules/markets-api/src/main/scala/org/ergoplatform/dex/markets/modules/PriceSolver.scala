package org.ergoplatform.dex.markets.modules

import org.ergoplatform.dex.domain.{AssetAmount, AssetEquiv, ValueUnits}

trait PriceSolver[F[_]] {

  def solve(asset: AssetAmount, targetUnits: ValueUnits): F[AssetEquiv]
}
