package org.ergoplatform.dex.matcher

import cats.data.NonEmptyList
import org.ergoplatform.dex.domain.Order

abstract class OrderBook[F[_]] {

  def process(orders: NonEmptyList[Order]): F[List[Match]]
}
