package org.ergoplatform.dex.markets.api.v1.models.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.common.models.TimeWindow
import org.ergoplatform.dex.domain.FullAsset
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.dex.markets.currencies.UsdUnits
import org.ergoplatform.dex.markets.db.models.amm.PoolSnapshot
import org.ergoplatform.dex.markets.domain.{FeePercentProjection, Fees, TotalValueLocked, Volume}
import sttp.tapir.Schema

@derive(encoder, decoder)
final case class PoolSummary(
  id: PoolId,
  lockedX: FullAsset,
  lockedY: FullAsset,
  tvl: TotalValueLocked,
  volume: Volume,
  fees: Fees,
  yearlyFeesPercent: FeePercentProjection
)

object PoolSummary {
  implicit val schema: Schema[PoolSummary] = Schema.derived

  def empty(pool: PoolSnapshot, window: TimeWindow): PoolSummary = PoolSummary(
    pool.id,
    pool.lockedX,
    pool.lockedY,
    TotalValueLocked.empty(UsdUnits),
    Volume.empty(UsdUnits, window),
    Fees.empty(UsdUnits, window),
    FeePercentProjection.empty
  )
}
