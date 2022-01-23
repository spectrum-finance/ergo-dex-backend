package org.ergoplatform.dex.domain.locks

import cats.effect.Sync
import derevo.cats.show
import derevo.circe.{decoder, encoder}
import derevo.derive
import doobie.{Get, Put}
import fs2.kafka.serde.{deserializerByDecoder, serializerByEncoder}
import fs2.kafka.{RecordDeserializer, RecordSerializer}
import io.estatico.newtype.macros.newtype
import org.ergoplatform.ergo.BoxId
import sttp.tapir.{Codec, Schema, Validator}
import tofu.logging.derivation.loggable

object types {

  @derive(show, loggable, encoder, decoder)
  @newtype final case class LockId(value: BoxId) {
    def unwrapped: String = value.value
  }

  object LockId {

    implicit val put: Put[LockId] = deriving
    implicit val get: Get[LockId] = deriving

    implicit def plainCodec: Codec.PlainCodec[LockId] = deriving

    implicit def codec: scodec.Codec[LockId] = deriving

    implicit def schema: Schema[LockId] =
      Schema.schemaForString.description("Lock ID").asInstanceOf[Schema[LockId]]

    implicit def validator: Validator[LockId] =
      implicitly[Validator[BoxId]].contramap[LockId](_.value)

    implicit def recordSerializer[F[_]: Sync]: RecordSerializer[F, LockId]     = serializerByEncoder
    implicit def recordDeserializer[F[_]: Sync]: RecordDeserializer[F, LockId] = deserializerByDecoder

    def fromStringUnsafe(s: String): LockId = LockId(BoxId.fromStringUnsafe(s))

    def fromBoxId(boxId: BoxId): LockId = LockId(boxId)
  }
}
