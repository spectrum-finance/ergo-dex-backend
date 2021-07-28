package org.ergoplatform.dex.domain

import cats.effect.Sync
import derevo.cats.show
import derevo.circe.{decoder, encoder}
import derevo.derive
import doobie.{Get, Put}
import fs2.kafka.serde.{deserializerByDecoder, serializerByEncoder}
import fs2.kafka.{RecordDeserializer, RecordSerializer}
import io.estatico.newtype.macros.newtype
import org.ergoplatform.common.HexString
import org.ergoplatform.ergo.{BoxId, TokenId}
import scodec.bits.ByteVector
import sttp.tapir.{Codec, Schema, Validator}
import tofu.logging.derivation.loggable

package object amm {

  @derive(show, loggable, encoder, decoder)
  @newtype final case class PoolId(value: TokenId) {
    def unwrapped: String = value.unwrapped
  }

  object PoolId {
    implicit def plainCodec: Codec.PlainCodec[PoolId] = deriving

    implicit def codec: scodec.Codec[PoolId] =
      scodec.codecs
        .bytes(32)
        .xmap(xs => fromBytes(xs.toArray), pid => ByteVector(pid.value.value.toBytes))

    implicit def schema: Schema[PoolId] =
      Schema.schemaForString.description("Pool ID").asInstanceOf[Schema[PoolId]]

    implicit def validator: Validator[PoolId] =
      implicitly[Validator[TokenId]].contramap[PoolId](_.value)

    implicit def recordSerializer[F[_]: Sync]: RecordSerializer[F, PoolId]     = serializerByEncoder
    implicit def recordDeserializer[F[_]: Sync]: RecordDeserializer[F, PoolId] = deserializerByDecoder

    def fromBytes(bytes: Array[Byte]): PoolId =
      PoolId(TokenId.fromBytes(bytes))

    def fromHex(s: HexString): PoolId = PoolId(TokenId(s))

    def fromStringUnsafe(s: String): PoolId = PoolId(TokenId.fromStringUnsafe(s))
  }

  @derive(show, loggable, encoder, decoder)
  @newtype case class OrderId(value: String)

  object OrderId {

    def fromBoxId(boxId: BoxId): OrderId = OrderId(boxId.value)

    implicit val get: Get[OrderId] = deriving
    implicit val put: Put[OrderId] = deriving

    implicit def recordSerializer[F[_]: Sync]: RecordSerializer[F, OrderId]     = serializerByEncoder
    implicit def recordDeserializer[F[_]: Sync]: RecordDeserializer[F, OrderId] = deserializerByDecoder
  }
}
