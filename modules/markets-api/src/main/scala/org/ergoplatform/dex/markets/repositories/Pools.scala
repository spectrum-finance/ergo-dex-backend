package org.ergoplatform.dex.markets.repositories

import org.ergoplatform.dex.markets.db.models.{PoolSnapshot, PoolVolumeSnapshot}
import org.ergoplatform.ergo.TokenId

trait Pools[F[_]] {

  /** Get snapshots of all pools.
    */
  def snapshots: F[List[PoolSnapshot]]

  /** Get snapshots of those pools that involve the given asset.
    */
  def snapshotsByAsset(asset: TokenId): F[List[PoolSnapshot]]

  /** Get recent volumes by all pools.
    */
  def volumes(fromTs: Long): F[List[PoolVolumeSnapshot]]
}
