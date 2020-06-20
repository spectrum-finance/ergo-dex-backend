package org.ergoplatform.dex.domain.syntax

import cats.data.NonEmptyList
import org.ergoplatform.dex.domain.models.Order.AnyOrder
import org.ergoplatform.dex.domain.models.{Match, OrderType}

object match_ {

  implicit final class MatchOps[T1 <: OrderType, T2 <: OrderType](private val m: Match[T1, T2]) extends AnyVal {
    def allOrders: NonEmptyList[AnyOrder] = m.order :: m.counterOrders
  }
}
