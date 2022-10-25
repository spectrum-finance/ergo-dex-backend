package org.ergoplatform.dex.markets.api.v1.models.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.dex.domain.FullAsset
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.dex.markets.domain.{FeePercentProjection, Fees, TotalValueLocked, Volume}
import sttp.tapir.Schema

@derive(encoder, decoder)
final case class PoolStats(
  id: PoolId,
  lockedX: FullAsset,
  lockedY: FullAsset,
  tvl: TotalValueLocked,
  volume: Volume,
  fees: Fees,
  yearlyFeesPercent: FeePercentProjection
)

object PoolStats {
  implicit val schema: Schema[PoolStats] = Schema.derived
}
