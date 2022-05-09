package org.ergoplatform.dex.markets.repositories

import cats.tagless.syntax.functorK._
import cats.{FlatMap, Functor}
import derevo.derive
import doobie.ConnectionIO
import org.ergoplatform.common.models.TimeWindow
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.dex.markets.db.models.amm._
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
trait Orders[F[_]] {

}

object Orders {

  def make[I[_]: Functor, D[_]: FlatMap: LiftConnectionIO](implicit
                                                           elh: EmbeddableLogHandler[D],
                                                           logs: Logs[I, D]
                                                          ): I[Orders[D]] =
    logs.forService[Orders[D]].map { implicit l =>
      elh.embed(implicit lh => new OrdersTracing[D] attach new Live(new AnalyticsSql()).mapK(LiftConnectionIO[D].liftF))
    }

  final class Live(sql: AnalyticsSql) extends Orders[ConnectionIO] {

  }

  final class OrdersTracing[F[_]: FlatMap: Logging] extends Orders[Mid[F, *]] {


  }
}
