package org.ergoplatform.dex.markets.repositories

import cats.data.NonEmptyList
import cats.tagless.syntax.functorK._
import cats.{Apply, FlatMap, Functor}
import derevo.derive
import doobie.ConnectionIO
import doobie.util.log.LogHandler
import org.ergoplatform.dex.domain.PairId
import org.ergoplatform.dex.markets.db.models.PoolSnapshot
import org.ergoplatform.dex.markets.db.sql.AnalyticsSql
import tofu.doobie.LiftConnectionIO
import tofu.doobie.log.EmbeddableLogHandler
import tofu.higherKind.Mid
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._

@derive(representableK)
trait AnalyticsRepo[D[_]] {

  def getPoolSnapshots: D[List[PoolSnapshot]]

}

object AnalyticsRepo {

  def make[I[_]: Functor, D[_]: FlatMap: LiftConnectionIO](implicit
    elh: EmbeddableLogHandler[D],
    logs: Logs[I, D]
  ): I[AnalyticsRepo[D]] =
    logs.forService[AnalyticsRepo[D]].map { implicit l =>
      elh.embed(implicit lh => new Live(new AnalyticsSql).mapK(LiftConnectionIO[D].liftF))
    }

  final class Live(sql: AnalyticsSql)(implicit lh: LogHandler) extends AnalyticsRepo[ConnectionIO] {

    def getPoolSnapshots: ConnectionIO[List[PoolSnapshot]] =
      sql.getPoolSnapshots.to[List]

  }

}
