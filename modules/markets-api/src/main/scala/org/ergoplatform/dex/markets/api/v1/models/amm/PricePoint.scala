package org.ergoplatform.dex.markets.api.v1.models.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import sttp.tapir.Schema

@derive(encoder, decoder)
case class PricePoint(
  height: Long,
  price: BigDecimal
                     )

object PricePoint {
  implicit val schema: Schema[PricePoint] = Schema.derived
}