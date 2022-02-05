package org.ergoplatform.dex.markets.api.v1.models.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.ergo.TokenId
import sttp.tapir.Schema

@derive(encoder, decoder)
case class PoolStats(
  baseId: TokenId,
  baseSymbol: String,
  quoteId: TokenId,
  quoteSymbol: String,
  lastPrice: BigDecimal,
  baseVolume: BigDecimal,
  quoteVolume: BigDecimal
)

object PoolStats {
  implicit val poolStatsSchema: Schema[PoolStats] = Schema.derived
}
