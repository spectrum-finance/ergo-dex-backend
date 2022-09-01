package org.ergoplatform.dex.markets.api.v1.models.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import sttp.tapir.Schema
import tofu.logging.derivation.loggable

@derive(loggable, decoder, encoder)
final case class OffChainCharts(address: String, spfReward: BigDecimal)

object OffChainCharts {

  implicit val schemaOffChainCharts: Schema[OffChainCharts] = Schema.derived
}
