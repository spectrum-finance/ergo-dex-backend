package org.ergoplatform.dex.domain

import cats.effect.Sync
import doobie.{Get, Put}
import fs2.kafka.serde.{deserializerViaKafkaDecoder}
import fs2.kafka.serde.ser._
import fs2.kafka.{RecordDeserializer, RecordSerializer}
import io.circe.{Decoder, Encoder}
import io.estatico.newtype.macros.newtype
import tofu.logging.Loggable

package object orderbook {

  @newtype case class OrderId(value: String)

  object OrderId {

    implicit val loggable: Loggable[OrderId] = deriving

    implicit val get: Get[OrderId] = deriving
    implicit val put: Put[OrderId] = deriving

    // circe instances
    implicit val encoder: Encoder[OrderId] = deriving
    implicit val decoder: Decoder[OrderId] = deriving

    implicit def recordSerializer[F[_]: Sync]: RecordSerializer[F, OrderId]     = serializerViaCirceEncoder
    implicit def recordDeserializer[F[_]: Sync]: RecordDeserializer[F, OrderId] = deserializerViaKafkaDecoder
  }

  @newtype case class TradeId(value: String)

  object TradeId {

    implicit val loggable: Loggable[TradeId] = deriving

    implicit val get: Get[TradeId] = deriving
    implicit val put: Put[TradeId] = deriving

    // circe instances
    implicit val encoder: Encoder[TradeId] = deriving
    implicit val decoder: Decoder[TradeId] = deriving

    implicit def recordSerializer[F[_]: Sync]: RecordSerializer[F, TradeId]     = serializerViaCirceEncoder
    implicit def recordDeserializer[F[_]: Sync]: RecordDeserializer[F, TradeId] = deserializerViaKafkaDecoder
  }
}
