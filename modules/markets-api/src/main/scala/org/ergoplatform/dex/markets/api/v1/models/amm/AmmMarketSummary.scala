package org.ergoplatform.dex.markets.api.v1.models.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.dex.domain.{MarketId, Ticker}
import org.ergoplatform.dex.markets.api.v1.models.amm.types.RealPrice
import org.ergoplatform.dex.markets.domain.CryptoVolume
import org.ergoplatform.ergo.TokenId
import sttp.tapir.Schema

@derive(encoder, decoder)
case class AmmMarketSummary(
  id: MarketId,
  baseId: TokenId,
  baseSymbol: Option[Ticker],
  quoteId: TokenId,
  quoteSymbol: Option[Ticker],
  lastPrice: RealPrice,
  baseVolume: CryptoVolume,
  quoteVolume: CryptoVolume
)

object AmmMarketSummary {
  implicit val poolStatsSchema: Schema[AmmMarketSummary] = Schema.derived
}
