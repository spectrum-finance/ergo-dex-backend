package org.ergoplatform.dex.matcher

import cats.syntax.apply._
import cats.{FlatMap, Monad}
import mouse.anyf._
import org.ergoplatform.dex.PairId
import org.ergoplatform.dex.domain.models.Match.AnyMatch
import org.ergoplatform.dex.domain.models.Order._
import org.ergoplatform.dex.domain.syntax.order._
import org.ergoplatform.dex.matcher.repositories.OrdersRepo
import tofu.doobie.transactor.Txr

final class LimitOrderBook[F[_]: FlatMap, D[_]: Monad](
  repo: OrdersRepo[D],
  trans: Txr.Aux[F, D]
) extends OrderBook[F] {

  implicit private val sellOrd: Ordering[SellOrder] = Ordering.by(o => (o.price, -o.feePerToken))
  implicit private val buyOrd: Ordering[BuyOrder]   = Ordering.by(o => (-o.price, -o.feePerToken))

  def process(pairId: PairId)(orders: List[AnyOrder]): F[List[AnyMatch]] = {
    val (sell, buy)                 = orders.partitioned
    val List(sellDemand, buyDemand) = List(sell, buy).map(_.map(_.amount).sum)
    (repo.getSellWall(pairId, buyDemand), repo.getBuyWall(pairId, sellDemand))
      .mapN((oldSell, oldBuy) => mkMatch(oldSell ++ sell, oldBuy ++ buy)) ||> trans.trans
  }

  private def mkMatch(
    sellOrders: List[SellOrder],
    buyOrders: List[BuyOrder]
  ): List[AnyMatch] = ???
}
