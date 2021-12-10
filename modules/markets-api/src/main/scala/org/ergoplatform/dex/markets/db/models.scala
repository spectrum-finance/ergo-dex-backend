package org.ergoplatform.dex.markets.db

import org.ergoplatform.dex.domain.{AssetAmount, Ticker}
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.ergo.TokenId

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

  final case class AssetInfo(
    id: TokenId,
    ticker: Option[Ticker],
    description: Option[String],
    decimals: Option[Int]
  )
}
