package org.ergoplatform.dex.matcher

import cats.syntax.functor._
import cats.instances.tuple._
import org.ergoplatform.dex.PairId
import org.ergoplatform.dex.domain.Match
import org.ergoplatform.dex.domain.Order.AnyOrder
import org.ergoplatform.dex.matcher.repositories.OrdersRepo

final class LimitOrderBook[F[_]](repo: OrdersRepo[F])(implicit ord: Ordering[AnyOrder])
  extends OrderBook[F] {

  def process(pairId: PairId)(orders: List[AnyOrder]): F[List[Match]] = {
    val xs @ (sell, buy)        = orders.sorted.partition(_.`type`.isSell)
    val (sellDemand, buyDemand) = xs.map(_.map(_.amount).sum)
    ???
  }
}
