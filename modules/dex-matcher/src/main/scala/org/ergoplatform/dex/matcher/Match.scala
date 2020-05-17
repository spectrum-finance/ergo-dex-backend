package org.ergoplatform.dex.matcher

import cats.data.NonEmptyList
import org.ergoplatform.dex.domain.Order

final case class Match(
  order: Order,
  matchedOrders: NonEmptyList[Order]
)
