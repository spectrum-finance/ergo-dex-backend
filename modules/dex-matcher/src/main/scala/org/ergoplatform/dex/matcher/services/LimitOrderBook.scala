package org.ergoplatform.dex.matcher.services

import cats.{FlatMap, Functor, Monad}
import mouse.anyf._
import org.ergoplatform.dex.PairId
import org.ergoplatform.dex.domain.models.Trade.AnyTrade
import org.ergoplatform.dex.domain.models.Order._
import org.ergoplatform.dex.domain.syntax.trade._
import org.ergoplatform.dex.domain.syntax.order._
import org.ergoplatform.dex.matcher.repositories.OrdersRepo
import tofu.doobie.transactor.Txr
import tofu.logging.{Logging, Logs}
import tofu.syntax.monadic._
import tofu.syntax.logging._

import scala.annotation.tailrec
import scala.language.existentials

final class LimitOrderBook[F[_]: FlatMap: Logging, D[_]: Monad](
  repo: OrdersRepo[D],
  txr: Txr.Aux[F, D]
) extends OrderBook[F] {

  import LimitOrderBook._

  def process(pairId: PairId)(orders: List[AnyOrder]): F[List[AnyTrade]] = {
    val (asks, bids)                = orders.partitioned
    val List(sellDemand, buyDemand) = List(asks, bids).map(_.map(_.amount).sum)
    info"Processing [${orders.size}] new orders of pair [$pairId]" >>
    (repo.getSellWall(pairId, buyDemand), repo.getBuyWall(pairId, sellDemand))
      .mapN((oldAsks, oldBids) => mkMatch(oldAsks ++ asks, oldBids ++ bids))
      .flatTap { matches =>
        val matched   = matches.flatMap(_.orders.map(_.id).toList)
        val unmatched = orders.filterNot(o => matched.contains(o.id))
        repo.remove(matched) >> repo.insert(unmatched)
      }
      .thrushK(txr.trans)
  }
}

object LimitOrderBook {

  implicit private val askOrd: Ordering[Ask] = Ordering.by(o => (o.price, -o.fee))
  implicit private val bidOrd: Ordering[Bid] = Ordering.by(o => (-o.price, -o.fee))

  def make[I[_]: Functor, F[_]: FlatMap, D[_]: Monad](
    repo: OrdersRepo[D],
    txr: Txr.Aux[F, D]
  )(implicit logs: Logs[I, F]): I[LimitOrderBook[F, D]] =
    logs.forService[LimitOrderBook[F, D]] map { implicit l =>
      new LimitOrderBook[F, D](repo, txr)
    }

  private[services] def mkMatch(
    asks: List[Ask],
    bids: List[Bid]
  ): List[AnyTrade] = {
    @tailrec def matchLoop(
      asks: List[Ask],
      bids: List[Bid],
      trades: List[AnyTrade]
    ): List[AnyTrade] =
      (asks.headOption, bids.headOption) match {
        case (Some(ask), Some(bid)) if ask.price <= bid.price =>
          if (ask.fee > bid.fee)
            ask fillWith bids match {
              case Some(anyTrade) =>
                matchLoop(
                  asks.tail,
                  bids.dropWhile(anyTrade.counterOrders.toList.contains), // todo: make sure orders compared correctly
                  anyTrade +: trades
                )
              case None =>
                trades
            }
          else
            bid fillWith asks match {
              case Some(anyTrade) =>
                matchLoop(
                  asks.dropWhile(anyTrade.counterOrders.toList.contains),
                  bids.tail,
                  anyTrade +: trades
                )
              case None =>
                trades
            }
        case _ => trades
      }
    matchLoop(asks.sorted, bids.sorted, Nil)
  }
}
