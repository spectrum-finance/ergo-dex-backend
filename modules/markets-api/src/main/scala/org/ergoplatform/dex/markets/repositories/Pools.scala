package org.ergoplatform.dex.markets.repositories

import org.ergoplatform.dex.markets.db.models.{PoolSnapshot, PoolVolumeSnapshot}
import org.ergoplatform.ergo.TokenId

trait Pools[F[_]] {

  def snapshots: F[List[PoolSnapshot]]

  def snapshotsByAsset(asset: TokenId): F[List[PoolSnapshot]]

  def volumes(fromTs: Long): F[List[PoolVolumeSnapshot]]
}
