package org.ergoplatform.dex.markets.api.v1.models.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import sttp.tapir.Schema
import tofu.logging.derivation.loggable

@derive(loggable, decoder, encoder)
final case class LpResultProd(
  address: String,
  spfReward: BigDecimal,
  totalWeight: BigDecimal,
  totalErgValue: BigDecimal,
  totalTime: String,
  totalOps: Int,
  pools: List[LpResultDev]
)

object LpResultProd {
  implicit val LpResultProdSchema: Schema[LpResultProd] = Schema.derived
}
