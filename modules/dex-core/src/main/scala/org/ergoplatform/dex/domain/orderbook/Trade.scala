package org.ergoplatform.dex.domain.orderbook

import cats.Show
import cats.data.NonEmptyList
import cats.effect.Sync
import cats.syntax.show._
import fs2.kafka.{RecordDeserializer, RecordSerializer}
import io.circe.{Decoder, Encoder}
import io.estatico.newtype.ops._
import org.ergoplatform.dex.domain.orderbook.FilledOrder.AnyFilledOrder
import org.ergoplatform.dex.{constants, TradeId}
import scorex.crypto.hash.Blake2b256
import scorex.util.encode.Base16
import tofu.logging.Loggable

final case class Trade[+T0 <: OrderType, +T1 <: OrderType](
  order: FilledOrder[T0],
  counterOrders: NonEmptyList[FilledOrder[T1]]
) {

  def id: TradeId =
    Base16
      .encode(Blake2b256.hash(orders.map(_.base.id.value.getBytes(constants.Charset)).toList.reduce(_ ++ _)))
      .coerce[TradeId]

  def orders: NonEmptyList[AnyFilledOrder] = order :: counterOrders
}

object Trade {

  type AnyTrade = Trade[OrderType, OrderType]

  implicit def show[T0 <: OrderType, T1 <: OrderType]: Show[Trade[T0, T1]] =
    trade =>
      s"Trade[${trade.order.base.`type`}, ${trade.counterOrders.head.base.`type`}](order=${trade.order.show}, " +
      s"counterOrders=${trade.counterOrders.show})"

  implicit def loggable[T0 <: OrderType, T1 <: OrderType]: Loggable[Trade[T0, T1]] =
    Loggable.stringValue.contramap[Trade[T0, T1]](_.toString)

  implicit def encoder: Encoder[AnyTrade] = io.circe.derivation.deriveEncoder
  implicit def decoder: Decoder[AnyTrade] = io.circe.derivation.deriveDecoder

  implicit def recordSerializer[F[_]: Sync]: RecordSerializer[F, AnyTrade] =
    fs2.kafka.serde.serializerByEncoder

  implicit def recordDeserializer[F[_]: Sync]: RecordDeserializer[F, AnyTrade] =
    fs2.kafka.serde.deserializerByDecoder
}
