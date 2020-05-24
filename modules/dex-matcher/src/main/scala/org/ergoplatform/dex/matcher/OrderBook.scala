package org.ergoplatform.dex.matcher

import org.ergoplatform.dex.domain.models.Match.AnyMatch
import org.ergoplatform.dex.domain.models.Order.AnyOrder

trait OrderBook[F[_]] {

  /** Match given orders with the current order book contents for a given `pairId`.
    */
  def process(orders: List[AnyOrder]): F[List[AnyMatch]]
}
