package org.ergoplatform.dex.markets.db.models

import org.ergoplatform.dex.domain.{FullAsset, Ticker}
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.ergo.TokenId

import java.text.SimpleDateFormat

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

  final case class AvgAssetAmount(timestamp: String, amountX: Long, amountY: Long)

  object AvgAssetAmounts {
    def empty: AvgAssetAmounts = AvgAssetAmounts(0, 0, 0, 0)
  }

  final case class AvgAssetAmountsWithPrev(
    current: String,
    prev: Option[String],
    amountX: Long,
    prevX: Option[Long],
    amountY: Long,
    prevY: Option[Long]
  ) {

    def getPrev(formatter: SimpleDateFormat): Option[AvgAssetAmounts] =
      for {
        ts <- prev
        x  <- prevX
        y  <- prevY
      } yield AvgAssetAmounts(x, y, formatter.parse(ts).getTime, 0)
  }

}
