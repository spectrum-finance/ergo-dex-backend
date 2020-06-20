package org.ergoplatform.dex.matcher.domain

import cats.{FlatMap, Monad}
import mouse.anyf._
import org.ergoplatform.dex.PairId
import org.ergoplatform.dex.domain.models.Match.AnyMatch
import org.ergoplatform.dex.domain.models.Order._
import org.ergoplatform.dex.domain.syntax.match_._
import org.ergoplatform.dex.domain.syntax.order._
import org.ergoplatform.dex.matcher.repositories.OrdersRepo
import tofu.doobie.transactor.Txr
import tofu.syntax.monadic._

import scala.annotation.tailrec
import scala.language.existentials

final class LimitOrderBook[F[_]: FlatMap, D[_]: Monad](
  repo: OrdersRepo[D],
  txr: Txr.Aux[F, D]
) extends OrderBook[F] {

  import LimitOrderBook._

  def process(pairId: PairId)(orders: List[AnyOrder]): F[List[AnyMatch]] = {
    val (sell, buy)                 = orders.partitioned
    val List(sellDemand, buyDemand) = List(sell, buy).map(_.map(_.amount).sum)
    (repo.getSellWall(pairId, buyDemand), repo.getBuyWall(pairId, sellDemand))
      .mapN((oldSell, oldBuy) => mkMatch(oldSell ++ sell, oldBuy ++ buy))
      .flatTap { matches =>
        val matched   = matches.flatMap(_.allOrders.map(_.id).toList)
        val unmatched = orders.filterNot(o => matched.contains(o.id))
        repo.remove(matched) >> repo.insert(unmatched)
      } ||> txr.trans
  }
}

object LimitOrderBook {

  implicit private val sellOrd: Ordering[SellOrder] = Ordering.by(o => (o.price, -o.fee))
  implicit private val buyOrd: Ordering[BuyOrder]   = Ordering.by(o => (-o.price, -o.fee))

  private def mkMatch(
    sellOrders: List[SellOrder],
    buyOrders: List[BuyOrder]
  ): List[AnyMatch] = {
    @tailrec def matchLoop(
      sellOrders: List[SellOrder],
      buyOrders: List[BuyOrder],
      matched: List[AnyMatch]
    ): List[AnyMatch] =
      (sellOrders.headOption, buyOrders.headOption) match {
        case (Some(sell), Some(buy)) if sell.price <= buy.price =>
          if (sell.fee > buy.fee)
            sell fillWith buyOrders match {
              case Some(anyMatch) =>
                matchLoop(
                  sellOrders.tail,
                  buyOrders.dropWhile(
                    anyMatch.counterOrders.toList.contains
                  ), // todo: make sure orders compared correctly
                  anyMatch +: matched
                )
              case None =>
                matched
            }
          else
            buy fillWith sellOrders match {
              case Some(anyMatch) =>
                matchLoop(
                  sellOrders.dropWhile(anyMatch.counterOrders.toList.contains),
                  buyOrders.tail,
                  anyMatch +: matched
                )
              case None =>
                matched
            }
        case _ => matched
      }
    matchLoop(sellOrders.sorted, buyOrders.sorted, Nil)
  }
}
