package org.ergoplatform.dex.domain.models

import cats.data.NonEmptyList
import shapeless.=:!=
import tofu.logging.Loggable

final case class Match[T0 <: OrderType, T1 <: OrderType](
  order: Order[T0],
  counterOrders: NonEmptyList[Order[T1]]
)(implicit ev: T0 =:!= T1)

object Match {

  type AnyMatch = Match[_ <: OrderType, _ <: OrderType]

  implicit def loggable[T0 <: OrderType, T1 <: OrderType]: Loggable[Match[T0, T1]] = ???
}
