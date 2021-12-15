package org.ergoplatform.dex.markets

import org.ergoplatform.dex.domain.FiatUnits

import scala.concurrent.duration.FiniteDuration

object domain {

  final case class TotalValueLocked(value: Long, units: FiatUnits)

  final case class Volume(value: Long, units: FiatUnits, period: FiniteDuration)

  final case class Fees(value: Long, units: FiatUnits, period: FiniteDuration)
}
