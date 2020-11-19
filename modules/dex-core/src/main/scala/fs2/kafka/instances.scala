package fs2.kafka

import cats.effect.Sync
import io.circe.{Decoder, Encoder}
import tofu.syntax.raise._
import tofu.syntax.monadic._
import io.circe.syntax._

object instances {

  private val charset = "UTF-8"

  implicit def deserializerByDecoder[F[_]: Sync, A: Decoder]: RecordDeserializer[F, A] =
    RecordDeserializer.lift {
      Deserializer.lift { xs =>
        val raw = new String(xs, charset)
        io.circe.parser.decode(raw).toRaise
      }
    }

  implicit def serializerByEncoder[F[_]: Sync, A: Encoder]: RecordSerializer[F, A] =
    RecordSerializer.lift {
      Serializer.lift { a =>
        a.asJson.noSpacesSortKeys.getBytes(charset).pure
      }
    }
}
