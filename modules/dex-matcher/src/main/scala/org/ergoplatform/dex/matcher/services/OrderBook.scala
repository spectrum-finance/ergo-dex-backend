package org.ergoplatform.dex.matcher.services

import org.ergoplatform.dex.domain.PairId
import org.ergoplatform.dex.domain.orderbook.Trade.AnyTrade
import org.ergoplatform.dex.domain.orderbook.Order.AnyOrder

trait OrderBook[F[_]] {

  /** Match given orders with the current order book contents for a given `pairId`.
    */
  def process(pairId: PairId)(orders: List[AnyOrder]): F[List[AnyTrade]]
}
