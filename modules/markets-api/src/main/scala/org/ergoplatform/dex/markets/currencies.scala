package org.ergoplatform.dex.markets

import org.ergoplatform.dex.domain.{Currency, FiatUnits}
import org.ergoplatform.ergo.CurrencyId

object currencies {

  val UsdTicker                 = "USD"
  val UsdDecimals               = 2
  val UsdCurrencyId: CurrencyId = CurrencyId(UsdTicker)
  val UsdCurrency: Currency     = Currency(UsdCurrencyId, UsdDecimals)
  val UsdUnits: FiatUnits       = FiatUnits(UsdCurrency)
}
