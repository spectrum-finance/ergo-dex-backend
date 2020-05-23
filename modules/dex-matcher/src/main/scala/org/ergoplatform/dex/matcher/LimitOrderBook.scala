package org.ergoplatform.dex.matcher

import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.{FlatMap, Monad}
import mouse.anyf._
import org.ergoplatform.dex.domain.models.Order._
import org.ergoplatform.dex.domain.models.Match
import org.ergoplatform.dex.domain.syntax.order._
import org.ergoplatform.dex.matcher.context.HasPairId
import org.ergoplatform.dex.matcher.repositories.OrdersRepo
import org.ergoplatform.dex.{PairId, Trans}
import tofu.syntax.context.{context => ctx}

final class LimitOrderBook[F[_]: HasPairId: FlatMap, D[_]: Monad](
  repo: OrdersRepo[D],
  trans: D Trans F
) extends OrderBook[F] {

  implicit private val sellOrd: Ordering[SellOrder] = Ordering.by(o => (o.price, -o.feePerToken))
  implicit private val buyOrd: Ordering[BuyOrder]   = Ordering.by(o => (-o.price, -o.feePerToken))

  def process(orders: List[AnyOrder]): F[List[Match]] = {
    val (sell, buy)                 = orders.partitioned
    val List(sellDemand, buyDemand) = List(sell, buy).map(_.map(_.amount).sum)
    val matchWithExisting =
      (pairId: PairId) =>
        (repo.getSellWall(pairId, buyDemand), repo.getBuyWall(pairId, sellDemand))
          .mapN((oldSell, oldBuy) => mkMatch(oldSell ++ sell, oldBuy ++ buy)) ||> trans
    ctx >>= matchWithExisting
  }

  private def mkMatch(
    sellOrders: List[SellOrder],
    buyOrders: List[BuyOrder]
  ): List[Match] = ???
}
