package org.ergoplatform.dex.markets.api.v1.models.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.dex.domain.Ticker
import org.ergoplatform.dex.markets.api.v1.models.amm.types.RealPrice
import org.ergoplatform.dex.markets.domain.CryptoVolume
import org.ergoplatform.ergo.TokenId
import sttp.tapir.Schema

@derive(encoder, decoder)
final case class PoolSummary(
  baseId: TokenId,
  baseName: Ticker,
  baseSymbol: Ticker,
  quoteId: TokenId,
  quoteName: Ticker,
  quoteSymbol: Ticker,
  lastPrice: RealPrice,
  baseVolume: CryptoVolume,
  quoteVolume: CryptoVolume
)

object PoolSummary {
  implicit val schema: Schema[PoolSummary] = Schema.derived
}
