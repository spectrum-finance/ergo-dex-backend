package org.ergoplatform.dex.markets.api.v1.models.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import sttp.tapir.Schema

@derive(encoder, decoder)
case class PoolSlippage(
  slippagePercent: BigDecimal
)

object PoolSlippage {
  implicit val schema: Schema[PoolSlippage] = Schema.derived
}
