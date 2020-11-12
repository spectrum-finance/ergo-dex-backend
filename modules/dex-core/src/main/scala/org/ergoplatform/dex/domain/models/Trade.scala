package org.ergoplatform.dex.domain.models

import cats.Show
import cats.data.NonEmptyList
import cats.syntax.show._
import io.estatico.newtype.ops._
import org.ergoplatform.dex.TradeId
import org.ergoplatform.dex.domain.models.Order.AnyOrder
import scorex.crypto.hash.Blake2b256
import scorex.util.encode.Base16
import shapeless.=:!=
import tofu.logging.Loggable

final case class Trade[T0 <: OrderType, T1 <: OrderType](
  order: Order[T0],
  counterOrders: NonEmptyList[Order[T1]]
)(implicit ev: T0 =:!= T1) {

  def id: TradeId =
    Base16.encode(Blake2b256.hash(orders.map(_.id.value.getBytes("UTF-8")).toList.reduce(_ ++ _))).coerce[TradeId]

  def orders: NonEmptyList[AnyOrder] = order :: counterOrders
}

object Trade {

  type AnyTrade = Trade[_ <: OrderType, _ <: OrderType]

  implicit def show[T0 <: OrderType, T1 <: OrderType]: Show[Trade[T0, T1]] =
    trade =>
      s"Trade[${trade.order.`type`}, ${trade.counterOrders.head.`type`}](order=${trade.order.show}, " +
      s"counterOrders=${trade.counterOrders.show})"

  implicit def loggable[T0 <: OrderType, T1 <: OrderType]: Loggable[Trade[T0, T1]] =
    Loggable.stringValue.contramap[Trade[T0, T1]](_.toString)
}
