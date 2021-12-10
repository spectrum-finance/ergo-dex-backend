package org.ergoplatform.dex.markets

import org.ergoplatform.dex.domain.ValueUnits

import scala.concurrent.duration.FiniteDuration

object domain {

  final case class TotalValueLocked(value: Long, units: ValueUnits)

  final case class Volume(value: Long, units: ValueUnits, period: FiniteDuration)

  final case class Fees(value: Long, units: ValueUnits, period: FiniteDuration)
}
