package org.ergoplatform.dex.domain

import cats.data.NonEmptyList
import org.ergoplatform.dex.domain.Order.AnyOrder

final case class Match(
  order: AnyOrder,
  matchedOrders: NonEmptyList[AnyOrder]
)
