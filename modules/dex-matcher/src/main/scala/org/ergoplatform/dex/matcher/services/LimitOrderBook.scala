package org.ergoplatform.dex.matcher.services

import cats.{FlatMap, Functor, Monad}
import cats.syntax.list._
import cats.syntax.traverse._
import cats.instances.option._
import mouse.anyf._
import org.ergoplatform.dex.domain.PairId
import org.ergoplatform.dex.domain.orderbook.Order._
import org.ergoplatform.dex.domain.orderbook.Trade.AnyTrade
import org.ergoplatform.dex.domain.syntax.order._
import org.ergoplatform.dex.matcher.modules.MatchingAlgo
import org.ergoplatform.dex.matcher.repositories.OrdersRepo
import tofu.doobie.transactor.Txr
import tofu.logging.{Logging, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._

import scala.language.existentials

final class LimitOrderBook[F[_]: FlatMap: Logging, D[_]: Monad](implicit
  matcher: MatchingAlgo[D],
  repo: OrdersRepo[D],
  txr: Txr.Aux[F, D]
) extends OrderBook[F] {

  def process(pairId: PairId)(orders: List[AnyOrder]): F[List[AnyTrade]] = {
    val (asks, bids)                = orders.partitioned
    val List(sellDemand, buyDemand) = List(asks, bids).map(_.map(_.amount).sum)
    info"Processing [${orders.size}] new orders of pair [$pairId]" >>
    (repo.getSellWall(pairId, buyDemand), repo.getBuyWall(pairId, sellDemand)).tupled
      .flatMap { case (oldAsks, oldBids) => matcher(oldAsks ++ asks, oldBids ++ bids) }
      .flatTap { trades =>
        val matched   = trades.flatMap(_.orders.map(_.base.id).toList)
        val unmatched = orders.filterNot(o => matched.contains(o.id))
        matched.toNel.fold(unit[D])(repo.remove) >> unmatched.toNel.fold(unit[D])(repo.insert)
      }
      .thrushK(txr.trans)
  }
}

object LimitOrderBook {

  def make[I[_]: Functor, F[_]: FlatMap, D[_]: Monad](implicit
    repo: OrdersRepo[D],
    txr: Txr.Aux[F, D],
    logs: Logs[I, F]
  ): I[LimitOrderBook[F, D]] =
    logs.forService[LimitOrderBook[F, D]] map { implicit l =>
      new LimitOrderBook[F, D]
    }
}
