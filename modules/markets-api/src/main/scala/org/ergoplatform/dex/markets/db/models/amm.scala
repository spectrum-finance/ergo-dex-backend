package org.ergoplatform.dex.markets.db.models

import org.ergoplatform.dex.domain.{FullAsset, Ticker}
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.ergo.TokenId

object amm {

  final case class PoolInfo(confirmedAt: Long)

  final case class TransactionInfo(value: Long)

  final case class AssetInfo(
    id: TokenId,
    ticker: Option[Ticker],
    decimals: Option[Int]
  ) {
    def evalDecimals: Int = decimals.getOrElse(0)
  }

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

  final case class PoolTrace(
    id: PoolId,
    lockedX: FullAsset,
    lockedY: FullAsset,
    height: Long,
    gindex: Long
  )

  final case class AvgAssetAmounts(
    amountX: Long,
    amountY: Long,
    timestamp: Long,
    index: Long
  )
}
