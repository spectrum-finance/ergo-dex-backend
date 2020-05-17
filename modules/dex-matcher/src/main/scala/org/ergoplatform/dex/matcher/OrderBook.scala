package org.ergoplatform.dex.matcher

import org.ergoplatform.dex.domain.Order

trait OrderBook[F[_]] {

  def process(orders: List[Order]): F[List[Match]]
}
