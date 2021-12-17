package org.ergoplatform.dex.markets.api.v1.models.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.dex.domain.FullAsset
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.dex.markets.domain.{Fees, TotalValueLocked, Volume}
import sttp.tapir.Schema

@derive(encoder, decoder)
final case class PoolSummary(
  id: PoolId,
  lockedX: FullAsset,
  lockedY: FullAsset,
  tvl: TotalValueLocked,
  volume: Volume,
  fees: Fees
)

object PoolSummary {
  implicit val schema: Schema[PoolSummary] = Schema.derived
}
