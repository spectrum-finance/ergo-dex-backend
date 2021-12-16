package org.ergoplatform.dex.markets.services

import org.ergoplatform.dex.domain.{AssetClass, FiatUnits}

trait FiatRates[F[_]] {

  def rateOf(asset: AssetClass, units: FiatUnits): F[Option[BigDecimal]]
}
