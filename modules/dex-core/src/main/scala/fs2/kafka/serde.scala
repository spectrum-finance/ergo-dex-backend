package fs2.kafka

import cats.effect.Sync
import io.circe.Encoder
import io.circe.syntax._
import tofu.syntax.monadic._

object serde {

  private val charset = "UTF-8"

  implicit def deserializerViaKafkaDecoder[F[_]: Sync, A](implicit
    decoder: KafkaDecoder[A, F]
  ): RecordDeserializer[F, A] =
    RecordDeserializer.lift {
      Deserializer.lift(decoder.decode)
    }

  implicit def serializerViaCirceEncoder[F[_]: Sync, A: Encoder]: RecordSerializer[F, A] =
    RecordSerializer.lift {
      Serializer.lift { a =>
        a.asJson.noSpacesSortKeys.getBytes(charset).pure
      }
    }
}
