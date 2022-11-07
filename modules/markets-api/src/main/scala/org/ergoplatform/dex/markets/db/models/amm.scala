package org.ergoplatform.dex.markets.db.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.dex.domain.{FullAsset, Ticker}
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.ergo.TokenId
import tofu.logging.derivation.loggable

object amm {

  final case class PoolInfo(confirmedAt: Long)

  final case class SwapInfo(asset: FullAsset, numTxs: Int)
  final case class DepositInfo(assetX: FullAsset, assetY: FullAsset, numTxs: Int)

  final case class AssetInfo(
    id: TokenId,
    ticker: Option[Ticker],
    decimals: Option[Int]
  ) {
    def evalDecimals: Int = decimals.getOrElse(0)
  }

  @derive(loggable)
  final case class PoolSnapshot(
    id: PoolId,
    lockedX: FullAsset,
    lockedY: FullAsset,
    dummy: String
  )

  @derive(loggable)
  final case class PoolFeeAndVolumeSnapshot(
    poolId: PoolId,
    volumeByXFee: FullAsset,
    volumeByYFee: FullAsset,
    volumeByXVolume: FullAsset,
    volumeByYVolume: FullAsset
  )

  @derive(loggable)
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
