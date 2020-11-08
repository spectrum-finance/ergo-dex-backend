package org.ergoplatform.dex.domain.models

import cats.Show
import cats.syntax.show._
import cats.data.NonEmptyList
import derevo.cats.show
import derevo.derive
import org.ergoplatform.dex.domain.models.Trade.loggable
import shapeless.=:!=
import tofu.logging.Loggable

final case class Trade[T0 <: OrderType, T1 <: OrderType](
  order: Order[T0],
  counterOrders: NonEmptyList[Order[T1]]
)(implicit ev: T0 =:!= T1)

object Trade {

  type AnyTrade = Trade[_ <: OrderType, _ <: OrderType]

  implicit def show[T0 <: OrderType, T1 <: OrderType]: Show[Trade[T0, T1]] =
    trade =>
      s"Trade[${trade.order.`type`}, ${trade.counterOrders.head.`type`}](order=${trade.order.show}, " +
        s"counterOrders=${trade.counterOrders.show})"

  implicit def loggable[T0 <: OrderType, T1 <: OrderType]: Loggable[Trade[T0, T1]] =
    Loggable.stringValue.contramap[Trade[T0, T1]](_.toString)
}
