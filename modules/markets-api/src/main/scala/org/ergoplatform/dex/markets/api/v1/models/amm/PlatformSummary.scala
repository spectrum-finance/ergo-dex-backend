package org.ergoplatform.dex.markets.api.v1.models.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.dex.markets.domain.{TotalValueLocked, Volume}
import sttp.tapir.Schema

@derive(encoder, decoder)
final case class PlatformSummary(tvl: TotalValueLocked, volume: Volume)

object PlatformSummary {
  implicit val schemaFees: Schema[PlatformSummary] = Schema.derived
}
