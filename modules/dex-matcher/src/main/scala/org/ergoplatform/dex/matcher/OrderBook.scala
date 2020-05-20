package org.ergoplatform.dex.matcher

import org.ergoplatform.dex.PairId
import org.ergoplatform.dex.domain.Order.AnyOrder
import org.ergoplatform.dex.domain.Match

trait OrderBook[F[_]] {

  /** Match given orders with the current order book contents for a given `pairId`.
    */
  def process(pairId: PairId)(orders: List[AnyOrder]): F[List[Match]]
}
