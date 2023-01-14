package org.ergoplatform.dex.markets.repositories

import cats.effect.Clock
import cats.tagless.syntax.functorK._
import cats.{FlatMap, Functor, Monad}
import derevo.derive
import doobie.ConnectionIO
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.dex.markets.db.models.locks.LiquidityLockStats
import org.ergoplatform.dex.markets.db.sql.LiquidityLocksSql
import org.ergoplatform.graphite.Metrics
import tofu.doobie.LiftConnectionIO
import tofu.doobie.log.EmbeddableLogHandler
import tofu.higherKind.Mid
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.time.now.millis

@derive(representableK)
trait Locks[F[_]] {

  /** Get liquidity locks by the pool with the given `id`.
    */
  def byPool(poolId: PoolId, leastDeadline: Int): F[List[LiquidityLockStats]]
}

object Locks {

  def make[I[_]: Functor, D[_]: Monad: Clock: LiftConnectionIO](implicit
    elh: EmbeddableLogHandler[D],
    metrics: Metrics[D],
    logs: Logs[I, D]
  ): I[Locks[D]] =
    logs.forService[Locks[D]].map { implicit l =>
      elh.embed(implicit lh =>
        new LocksTracing[D] attach (new LocksMetrics[D] attach new Live(new LiquidityLocksSql())
          .mapK(LiftConnectionIO[D].liftF))
      )
    }

  final class Live(sql: LiquidityLocksSql) extends Locks[ConnectionIO] {

    def byPool(poolId: PoolId, leastDeadline: Int): ConnectionIO[List[LiquidityLockStats]] =
      sql.getLocksByPool(poolId, leastDeadline).to[List]
  }

  final class LocksMetrics[F[_]: Monad: Clock](implicit metrics: Metrics[F]) extends Locks[Mid[F, *]] {

    private def processMetric[A](f: F[A], key: String): F[A] =
      for {
        start  <- millis
        r      <- f
        finish <- millis
        _      <- metrics.sendTs(key, finish - start)
        _      <- metrics.sendCount(key, 1)
      } yield r

    def byPool(poolId: PoolId, leastDeadline: Int): Mid[F, List[LiquidityLockStats]] =
      processMetric(_, s"db.locks.byPool.$poolId")
  }

  final class LocksTracing[F[_]: FlatMap: Logging] extends Locks[Mid[F, *]] {

    def byPool(poolId: PoolId, leastDeadline: Int): Mid[F, List[LiquidityLockStats]] =
      for {
        _ <- trace"byPool(poolId=$poolId, leastDeadline=$leastDeadline)"
        r <- _
        _ <- trace"byPool(poolId=$poolId, leastDeadline=$leastDeadline) -> ${r.size} info entities selected"
      } yield r
  }
}
