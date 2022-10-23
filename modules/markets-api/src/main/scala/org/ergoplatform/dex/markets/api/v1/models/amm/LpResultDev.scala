package org.ergoplatform.dex.markets.api.v1.models.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import sttp.tapir.Schema
import tofu.logging.derivation.loggable

@derive(loggable, decoder, encoder)
final case class LpResultDev(
  totalSpfReward: BigDecimal,
  totalWeight: BigDecimal,
  operations: Int,
  totalErgValue: BigDecimal,
  totalTime: BigDecimal,
  totalTimeHours: String,
  pool: String
)

object LpResultDev {
  implicit val LpResultDevSchema: Schema[LpResultDev] = Schema.derived
}
