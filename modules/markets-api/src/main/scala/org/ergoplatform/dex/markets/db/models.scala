package org.ergoplatform.dex.markets.db

import com.google.common.base.Ticker
import org.ergoplatform.dex.domain.{AssetAmount, FullAsset}
import org.ergoplatform.dex.domain.amm.{OrderId, PoolId}
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

  final case class PoolFeesSnapshot(
    poolId: PoolId,
    feesByX: FullAsset,
    feesByY: FullAsset
  )

  final case class SwapSnapshot(
    orderId: OrderId,
    poolId: PoolId,
    inputId: TokenId,
    inputValue: Long,
    inputTicker: Option[Ticker],
    minOutputId: TokenId,
    minOutputValue: Long,
    minOutputTicker: Option[Ticker],
    outputAmount: Long
  )
}
