package org.ergoplatform.dex.markets.repositories

import cats.{FlatMap, Functor}
import cats.tagless.syntax.functorK._
import derevo.derive
import doobie.ConnectionIO
import org.ergoplatform.common.models.TimeWindow
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.dex.markets.db.models.{PoolFeesSnapshot, PoolSnapshot, PoolVolumeSnapshot}
import org.ergoplatform.dex.markets.db.sql.AnalyticsSql
import org.ergoplatform.ergo.TokenId
import tofu.doobie.LiftConnectionIO
import tofu.doobie.log.EmbeddableLogHandler
import tofu.higherKind.derived.representableK
import tofu.logging.Logs
import tofu.syntax.monadic._

@derive(representableK)
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
  def volume(id: PoolId, window: TimeWindow): F[Option[PoolVolumeSnapshot]]

  /** Get fees by all pools.
    */
  def fees(window: TimeWindow): F[List[PoolFeesSnapshot]]

  /** Get fees by a given pool.
    */
  def fees(id: PoolId, window: TimeWindow): F[Option[PoolFeesSnapshot]]
}

object Pools {

  def make[I[_]: Functor, D[_]: FlatMap: LiftConnectionIO](implicit
    elh: EmbeddableLogHandler[D],
    logs: Logs[I, D]
  ): I[Pools[D]] =
    logs.forService[Pools[D]].map { implicit l =>
      elh.embed(implicit lh => new Live(new AnalyticsSql()).mapK(LiftConnectionIO[D].liftF))
    }

  final class Live(sql: AnalyticsSql) extends Pools[ConnectionIO] {

    def snapshots: ConnectionIO[List[PoolSnapshot]] =
      sql.getPoolSnapshots.to[List]

    def snapshotsByAsset(asset: TokenId): ConnectionIO[List[PoolSnapshot]] =
      sql.getPoolSnapshotsByAsset(asset).to[List]

    def snapshot(id: PoolId): ConnectionIO[Option[PoolSnapshot]] =
      sql.getPoolSnapshot(id).option

    def volumes(window: TimeWindow): ConnectionIO[List[PoolVolumeSnapshot]] =
      sql.getPoolVolumes(window).to[List]

    def volume(id: PoolId, window: TimeWindow): ConnectionIO[Option[PoolVolumeSnapshot]] =
      sql.getPoolVolumes(id, window).option

    def fees(window: TimeWindow): ConnectionIO[List[PoolFeesSnapshot]] =
      sql.getPoolFees(window).to[List]

    def fees(id: PoolId, window: TimeWindow): ConnectionIO[Option[PoolFeesSnapshot]] =
      sql.getPoolFees(id, window).option
  }
}
