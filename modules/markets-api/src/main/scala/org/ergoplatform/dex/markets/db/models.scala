package org.ergoplatform.dex.markets.db

import org.ergoplatform.dex.domain.Ticker
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.ergo.TokenId

object models {

  final case class PoolSnapshot(
    id: PoolId,
    lockedX: FullAsset,
    lockedY: FullAsset
  )

  final case class PoolVolumeSnapshot(
    poolId: PoolId,
    volumeByX: FullAsset,
    volumeByY: FullAsset
  )

  final case class FullAsset(
    id: TokenId,
    amount: Long,
    ticker: Option[Ticker],
    decimals: Option[Int]
  )
}
