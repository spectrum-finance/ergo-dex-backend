package org.ergoplatform.dex.markets.api.v1.models.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import sttp.tapir.Schema
import tofu.logging.derivation.loggable

@derive(loggable, decoder, encoder)
final case class LpResultDev(
  spfReward: BigDecimal,
  weight: BigDecimal,
  operations: Int,
  ergValue: BigDecimal,
  time: BigDecimal,
  timeHours: String,
  pool: String
)

object LpResultDev {
  implicit val LpResultDevSchema: Schema[LpResultDev] = Schema.derived
}
