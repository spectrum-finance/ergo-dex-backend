package org.ergoplatform.dex.markets.db.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.dex.domain.{FullAsset, Ticker}
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.dex.domain.{FullAsset, Ticker}
import org.ergoplatform.ergo.TokenId
import sttp.tapir.Schema
import tofu.logging.derivation.loggable

object amm {

  @derive(loggable)
  final case class OffChainOperatorState(operations: Int, totalReward: BigDecimal)

  @derive(loggable)
  final case class SwapState(inputId: String, inputValue: BigDecimal, outputAmount: BigDecimal)

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

  @derive(loggable, decoder, encoder)
  final case class DBLpState(
    poolId: String,
    txId: String,
    balance: BigDecimal,
    timestamp: Long,
    op: String,
    amount: String
  )

  object DBLpState {
    implicit val schema: Schema[DBLpState] = Schema.derived
  }

  @derive(loggable, decoder, encoder)
  final case class LqProviderStateDB(
    address: String,
    totalWeight: BigDecimal,
    totalCount: Int,
    totalErgValue: BigDecimal,
    totalTime: BigDecimal
  )

  object LqProviderStateDB {
    implicit val schema: Schema[LqProviderStateDB] = Schema.derived
    def empty(address: String): LqProviderStateDB = LqProviderStateDB(address, 0, 0, 0, 0)
  }
}
