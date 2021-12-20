package org.ergoplatform.dex.markets.repositories

import cats.tagless.syntax.functorK._
import cats.{FlatMap, Functor}
import derevo.derive
import doobie.ConnectionIO
import org.ergoplatform.common.models.TimeWindow
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.dex.markets.db.models.{PoolFeesSnapshot, PoolInfo, PoolSnapshot, PoolVolumeSnapshot}
import org.ergoplatform.dex.markets.db.sql.AnalyticsSql
import org.ergoplatform.ergo.TokenId
import tofu.doobie.LiftConnectionIO
import tofu.doobie.log.EmbeddableLogHandler
import tofu.higherKind.Mid
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._

@derive(representableK)
trait Pools[F[_]] {

  /** Get general info about the pool with the given `id`.
    */
  def info(id: PoolId): F[Option[PoolInfo]]

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
      elh.embed(implicit lh => new PoolsTracing[D] attach new Live(new AnalyticsSql()).mapK(LiftConnectionIO[D].liftF))
    }

  final class Live(sql: AnalyticsSql) extends Pools[ConnectionIO] {

    def info(id: PoolId): ConnectionIO[Option[PoolInfo]] =
      sql.getInfo(id).option

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

  final class PoolsTracing[F[_]: FlatMap: Logging] extends Pools[Mid[F, *]] {

    def info(poolId: PoolId): Mid[F, Option[PoolInfo]] =
      for {
        _ <- trace"info(poolId=$poolId)"
        r <- _
        _ <- trace"info(poolId=$poolId) -> ${r.size} info entities selected"
      } yield r

    def snapshots: Mid[F, List[PoolSnapshot]] =
      for {
        _ <- trace"snapshots"
        r <- _
        _ <- trace"snapshots -> ${r.size} snapshots selected"
      } yield r

    def snapshotsByAsset(asset: TokenId): Mid[F, List[PoolSnapshot]] =
      for {
        _ <- trace"snapshotsByAsset(asset=$asset)"
        r <- _
        _ <- trace"snapshotsByAsset(asset=$asset) -> ${r.size} snapshots selected"
      } yield r

    def snapshot(poolId: PoolId): Mid[F, Option[PoolSnapshot]] =
      for {
        _ <- trace"snapshot(poolId=$poolId)"
        r <- _
        _ <- trace"snapshot(poolId=$poolId) -> ${r.size} snapshots selected"
      } yield r

    def volumes(window: TimeWindow): Mid[F, List[PoolVolumeSnapshot]] =
      for {
        _ <- trace"volumes(window=$window)"
        r <- _
        _ <- trace"volumes(window=$window) -> ${r.size} volume snapshots selected"
      } yield r

    def volume(poolId: PoolId, window: TimeWindow): Mid[F, Option[PoolVolumeSnapshot]] =
      for {
        _ <- trace"volume(poolId=$poolId, window=$window)"
        r <- _
        _ <- trace"volume(poolId=$poolId, window=$window) -> ${r.size} volume snapshots selected"
      } yield r

    def fees(window: TimeWindow): Mid[F, List[PoolFeesSnapshot]] =
      for {
        _ <- trace"fees(window=$window)"
        r <- _
        _ <- trace"fees(window=$window) -> ${r.size} fees snapshots selected"
      } yield r

    def fees(poolId: PoolId, window: TimeWindow): Mid[F, Option[PoolFeesSnapshot]] =
      for {
        _ <- trace"fees(poolId=$poolId, window=$window)"
        r <- _
        _ <- trace"fees(poolId=$poolId, window=$window) -> ${r.size} fees snapshots selected"
      } yield r
  }
}
