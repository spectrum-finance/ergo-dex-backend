package org.ergoplatform.dex.markets.api.v1.models.amm

import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.dex.markets.domain.{Fees, TotalValueLocked, Volume}

final case class PoolSummary(
  id: PoolId,
  lockedX: AssetAmount,
  lockedY: AssetAmount,
  tvl: TotalValueLocked,
  volume: Volume,
  fees: Fees
)
