package org.ergoplatform.dex.domain.models

import cats.data.NonEmptyList
import shapeless.=:!=
import tofu.logging.Loggable

final case class Trade[T0 <: OrderType, T1 <: OrderType](
  order: Order[T0],
  counterOrders: NonEmptyList[Order[T1]]
)(implicit ev: T0 =:!= T1)

object Trade {

  type AnyTrade = Trade[_ <: OrderType, _ <: OrderType]

  implicit def loggable[T0 <: OrderType, T1 <: OrderType]: Loggable[Trade[T0, T1]] = ???
}
