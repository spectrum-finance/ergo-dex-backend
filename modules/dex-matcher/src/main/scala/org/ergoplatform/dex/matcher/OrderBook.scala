package org.ergoplatform.dex.matcher

import org.ergoplatform.dex.PairId
import org.ergoplatform.dex.domain.{Match, TaggedOrder}

trait OrderBook[F[_]] {

  def process(pairId: PairId)(orders: List[TaggedOrder]): F[List[Match]]
}
