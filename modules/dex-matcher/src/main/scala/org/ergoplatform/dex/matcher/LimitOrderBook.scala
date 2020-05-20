package org.ergoplatform.dex.matcher

import cats.syntax.functor._
import cats.instances.tuple._
import org.ergoplatform.dex.PairId
import org.ergoplatform.dex.domain.{Match, TaggedOrder}
import org.ergoplatform.dex.matcher.repositories.OrdersRepo

final class LimitOrderBook[F[_]](repo: OrdersRepo[F])(implicit ord: Ordering[TaggedOrder])
  extends OrderBook[F] {

  def process(pairId: PairId)(orders: List[TaggedOrder]): F[List[Match]] = ???
}
