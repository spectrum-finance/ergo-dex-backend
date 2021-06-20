package org.ergoplatform.dex.domain

import cats.effect.Sync
import doobie.{Get, Put}
import fs2.kafka.serde.{deserializerByDecoder, serializerByEncoder}
import fs2.kafka.{RecordDeserializer, RecordSerializer}
import io.circe.{Decoder, Encoder}
import io.estatico.newtype.macros.newtype
import org.ergoplatform.ergo.{BoxId, TokenId}
import sttp.tapir.{Codec, Schema, Validator}
import tofu.logging.Loggable

package object amm {

  @newtype final case class PoolId(value: TokenId) {
    def unwrapped: String = value.unwrapped
  }

  object PoolId {
    implicit val loggable: Loggable[PoolId] = deriving

    // circe instances
    implicit val encoder: Encoder[PoolId] = deriving
    implicit val decoder: Decoder[PoolId] = deriving

    implicit def plainCodec: Codec.PlainCodec[PoolId] = deriving

    implicit def schema: Schema[PoolId] =
      Schema.schemaForString.description("Pool ID").asInstanceOf[Schema[PoolId]]

    implicit def validator: Validator[PoolId] =
      implicitly[Validator[TokenId]].contramap[PoolId](_.value)

    implicit def recordSerializer[F[_]: Sync]: RecordSerializer[F, PoolId]     = serializerByEncoder
    implicit def recordDeserializer[F[_]: Sync]: RecordDeserializer[F, PoolId] = deserializerByDecoder

    def fromBytes(bytes: Array[Byte]): PoolId =
      PoolId(TokenId.fromBytes(bytes))
  }

  @newtype case class OperationId(value: String)

  object OperationId {

    def fromBoxId(boxId: BoxId): OperationId = OperationId(boxId.value)

    implicit val loggable: Loggable[OperationId] = deriving

    implicit val get: Get[OperationId] = deriving
    implicit val put: Put[OperationId] = deriving

    // circe instances
    implicit val encoder: Encoder[OperationId] = deriving
    implicit val decoder: Decoder[OperationId] = deriving

    implicit def recordSerializer[F[_]: Sync]: RecordSerializer[F, OperationId]     = serializerByEncoder
    implicit def recordDeserializer[F[_]: Sync]: RecordDeserializer[F, OperationId] = deserializerByDecoder
  }
}
