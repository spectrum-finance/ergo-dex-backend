package org.ergoplatform.dex.markets.repositories

import cats.tagless.syntax.functorK._
import cats.{FlatMap, Functor}
import derevo.derive
import doobie.ConnectionIO
import org.ergoplatform.common.models.{Paging, TimeWindow}
import org.ergoplatform.dex.markets.api.v1.models.amm.Order._
import org.ergoplatform.dex.markets.api.v1.models.amm.OrdersRequest
import org.ergoplatform.dex.markets.db.models.amm._
import org.ergoplatform.dex.markets.db.sql.AnalyticsSql
import tofu.doobie.LiftConnectionIO
import tofu.doobie.log.EmbeddableLogHandler
import tofu.higherKind.Mid
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._

@derive(representableK)
trait Orders[F[_]] {

  def getSwapTxs(tw: TimeWindow): F[List[SwapInfo]] //rename

  def getDepositTxs(tw: TimeWindow): F[List[DepositInfo]] //rename

  def getSwaps(paging: Paging, request: OrdersRequest): F[List[Swap]]

  def getDeposits(paging: Paging, request: OrdersRequest): F[List[Deposit]]

  def getRedeems(paging: Paging, request: OrdersRequest): F[List[Redeem]]

  def getLocks(paging: Paging, request: OrdersRequest): F[List[Lock]]

  def getAll(paging: Paging, request: OrdersRequest): F[List[AnyOrder]]

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

    def getSwapTxs(tw: TimeWindow): ConnectionIO[List[SwapInfo]] =
      sql.getSwapTransactions(tw).to[List]

    def getDepositTxs(tw: TimeWindow): ConnectionIO[List[DepositInfo]] =
      sql.getDepositTransactions(tw).to[List]

    def getSwaps(paging: Paging, request: OrdersRequest): ConnectionIO[List[Swap]] =


    def getDeposits(paging: Paging, request: OrdersRequest): ConnectionIO[List[Deposit]]

    def getRedeems(paging: Paging, request: OrdersRequest): ConnectionIO[List[Redeem]]

    def getLocks(paging: Paging, request: OrdersRequest): ConnectionIO[List[Lock]]

  }

  final class OrdersTracing[F[_]: FlatMap: Logging] extends Orders[Mid[F, *]] {

    def getSwapTxs(tw: TimeWindow): Mid[F, List[SwapInfo]] =
      for {
        _ <- trace"swaps(window=$tw)"
        r <- _
        _ <- trace"swaps(window=$tw) -> ${r.size} swap entities selected"
      } yield r

    def getDepositTxs(tw: TimeWindow): Mid[F, List[DepositInfo]] =
      for {
        _ <- trace"deposits(window=$tw)"
        r <- _
        _ <- trace"deposits(window=$tw) -> ${r.size} deposit entities selected"
      } yield r
  }
}
