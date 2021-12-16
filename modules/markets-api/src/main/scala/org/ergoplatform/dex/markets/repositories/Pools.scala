package org.ergoplatform.dex.markets.repositories

import org.ergoplatform.common.models.TimeWindow
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.dex.markets.db.models.{PoolFeesSnapshot, PoolSnapshot, PoolVolumeSnapshot}
import org.ergoplatform.ergo.TokenId

trait Pools[F[_]] {

  /** Get snapshots of all pools.
    */
  def snapshots: F[List[PoolSnapshot]]

  /** Get snapshots of those pools that involve the given asset.
    */
  def snapshotsByAsset(asset: TokenId): F[List[PoolSnapshot]]

  /** Get a snapshot of the pool with the given `id`.
    */
  def snapshot(id: PoolId): F[Option[PoolSnapshot]]

  /** Get recent volumes by all pools.
    */
  def volumes(window: TimeWindow): F[List[PoolVolumeSnapshot]]

  /** Get volumes by a given pool.
    */
  def poolVolume(id: PoolId, window: TimeWindow): F[Option[PoolVolumeSnapshot]]

  /** Get fees by a given pool.
    */
  def poolFees(id: PoolId, window: TimeWindow): F[Option[PoolFeesSnapshot]]
}
