package org.ergoplatform.dex.markets.db.models

import org.ergoplatform.dex.domain.FullAsset
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.ergo.TokenId

object amm {

  final case class PoolInfo(confirmedAt: Long)

  final case class PoolTokenInfo(
    baseId: TokenId,
    baseSymbol: String,
    quoteId: TokenId,
    quoteSymbol: String
  )

  final case class PoolSnapshot(
    id: PoolId,
    lockedX: FullAsset,
    lockedY: FullAsset,
    dummy: String
  )

  final case class PoolVolumeSnapshot(
    poolId: PoolId,
    volumeByX: FullAsset,
    volumeByY: FullAsset
  )

  final case class PoolFeesSnapshot(
    poolId: PoolId,
    feesByX: FullAsset,
    feesByY: FullAsset
  )
}
