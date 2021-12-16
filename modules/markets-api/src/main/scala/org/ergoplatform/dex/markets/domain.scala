package org.ergoplatform.dex.markets

import org.ergoplatform.common.models.TimeWindow
import org.ergoplatform.dex.domain.FiatUnits

object domain {

  final case class TotalValueLocked(value: BigDecimal, units: FiatUnits)

  final case class Volume(value: BigDecimal, units: FiatUnits, window: TimeWindow)

  final case class Fees(value: BigDecimal, units: FiatUnits, window: TimeWindow)
}
