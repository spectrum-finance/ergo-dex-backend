package fs2.kafka

import cats.Applicative
import cats.effect.Sync
import io.circe.Decoder
import org.ergoplatform.common.IsOption
import tofu.Throws
import tofu.syntax.monadic._
import tofu.syntax.raise._

trait KafkaDecoder[A, F[_]] {
  def decode(xs: Array[Byte]): F[A]
}

object KafkaDecoder extends KafkaDecoderLowPriority {

  implicit def optionalDeserializerByDecoder[A: Decoder, F[_]: Sync](implicit
    opt: IsOption[A]
  ): KafkaDecoder[A, F] =
    (xs: Array[Byte]) => {
      val raw = new String(xs, charset)
      io.circe.parser.decode(raw).toOption.getOrElse(opt.none).pure
    }
}

private[kafka] trait KafkaDecoderLowPriority {

  protected val charset = "UTF-8"

  implicit def deserializerByDecoder[A: Decoder, F[_]: Applicative: Throws]: KafkaDecoder[A, F] =
    (xs: Array[Byte]) => {
      val raw = new String(xs, charset)
      io.circe.parser.decode(raw).toRaise
    }
}
