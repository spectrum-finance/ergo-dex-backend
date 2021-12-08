package org.ergoplatform.dex.markets

import scala.concurrent.duration.FiniteDuration

object domain {

  final case class TotalValueLocked(value: Long, units: String)

  final case class Volume(value: Long, units: String, period: FiniteDuration)

  final case class Fees(value: Long, units: String, period: FiniteDuration)
}
