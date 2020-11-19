package org.ergoplatform.dex.domain.models

import cats.Show
import cats.data.NonEmptyList
import cats.effect.Sync
import cats.syntax.show._
import fs2.kafka.{RecordDeserializer, RecordSerializer}
import io.circe.{Decoder, Encoder}
import io.estatico.newtype.ops._
import org.ergoplatform.dex.{constants, TradeId}
import org.ergoplatform.dex.domain.models.Order.AnyOrder
import scorex.crypto.hash.Blake2b256
import scorex.util.encode.Base16
import shapeless.=:!=
import tofu.logging.Loggable

final case class Trade[+T0 <: OrderType, +T1 <: OrderType](
  order: Order[T0],
  counterOrders: NonEmptyList[Order[T1]]
) {

  def id: TradeId =
    Base16
      .encode(Blake2b256.hash(orders.map(_.id.value.getBytes(constants.Charset)).toList.reduce(_ ++ _)))
      .coerce[TradeId]

  def orders: NonEmptyList[AnyOrder] = order :: counterOrders
}

object Trade {

  type AnyTrade = Trade[OrderType, OrderType]

  implicit def show[T0 <: OrderType, T1 <: OrderType]: Show[Trade[T0, T1]] =
    trade =>
      s"Trade[${trade.order.`type`}, ${trade.counterOrders.head.`type`}](order=${trade.order.show}, " +
      s"counterOrders=${trade.counterOrders.show})"

  implicit def loggable[T0 <: OrderType, T1 <: OrderType]: Loggable[Trade[T0, T1]] =
    Loggable.stringValue.contramap[Trade[T0, T1]](_.toString)

  implicit def encoder: Encoder[AnyTrade] = io.circe.derivation.deriveEncoder
  implicit def decoder: Decoder[AnyTrade] = io.circe.derivation.deriveDecoder

  implicit def recordSerializer[F[_]: Sync]: RecordSerializer[F, AnyTrade] =
    fs2.kafka.instances.serializerByEncoder

  implicit def recordDeserializer[F[_]: Sync]: RecordDeserializer[F, AnyTrade] =
    fs2.kafka.instances.deserializerByDecoder
}
