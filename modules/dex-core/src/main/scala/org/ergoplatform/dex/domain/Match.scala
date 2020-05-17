package org.ergoplatform.dex.domain

import cats.data.NonEmptyList

final case class Match(
  order: Order,
  matchedOrders: NonEmptyList[Order]
)
