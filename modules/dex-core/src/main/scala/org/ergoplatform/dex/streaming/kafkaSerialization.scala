package org.ergoplatform.dex.streaming

import cats.effect.Sync
import fs2.kafka.Deserializer
import io.circe.Decoder

object kafkaSerialization {

  implicit def jsonValueDeserializer[F[_]: Sync, A: Decoder]: Deserializer[F, Option[A]] =
    Deserializer.string.map(x =>
      io.circe.parser.parse(x).toOption.flatMap(Decoder[A].decodeJson(_).toOption)
    )
}
