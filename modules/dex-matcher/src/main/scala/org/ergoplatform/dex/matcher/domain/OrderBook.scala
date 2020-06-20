package org.ergoplatform.dex.matcher.domain

import org.ergoplatform.dex.PairId
import org.ergoplatform.dex.domain.models.Match.AnyMatch
import org.ergoplatform.dex.domain.models.Order.AnyOrder

trait OrderBook[F[_]] {

  /** Match given orders with the current order book contents for a given `pairId`.
    */
  def process(pairId: PairId)(orders: List[AnyOrder]): F[List[AnyMatch]]
}
