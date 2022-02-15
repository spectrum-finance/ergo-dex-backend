package org.ergoplatform.dex.markets.api.v1.models.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import sttp.tapir.Schema

import scala.math.BigDecimal.RoundingMode

@derive(encoder, decoder)
case class PoolSlippage(
  slippagePercent: BigDecimal
) {
  def scale(value: Int): PoolSlippage = PoolSlippage(slippagePercent.setScale(value, RoundingMode.HALF_UP))
}

object PoolSlippage {
  implicit val schema: Schema[PoolSlippage] = Schema.derived
}
