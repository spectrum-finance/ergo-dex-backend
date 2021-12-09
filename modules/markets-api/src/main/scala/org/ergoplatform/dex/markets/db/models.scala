package org.ergoplatform.dex.markets.db

import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.amm.PoolId

object models {

  final case class PoolSnapshot(
    id: PoolId,
    lockedX: AssetAmount,
    lockedY: AssetAmount
  )

  final case class PoolVolumeSnapshot(
    poolId: PoolId,
    volumeByX: AssetAmount,
    volumeByY: AssetAmount
  )
}
