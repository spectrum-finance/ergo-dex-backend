package org.ergoplatform.dex.domain.models

import cats.data.NonEmptyList
import org.ergoplatform.dex.domain.models.Order.{BuyOrder, SellOrder}

final case class Match(
  sellOrders: NonEmptyList[SellOrder],
  buyOrders: NonEmptyList[BuyOrder]
)
