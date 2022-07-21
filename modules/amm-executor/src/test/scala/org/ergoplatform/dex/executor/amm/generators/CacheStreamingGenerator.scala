package org.ergoplatform.dex.executor.amm.generators

import cats.effect.IO
import org.ergoplatform.common.cache.CacheStreaming
import org.ergoplatform.dex.domain.amm.CFMMOrder
import org.ergoplatform.dex.executor.amm.services.StreamF
import scodec.Codec
import tofu.logging.Loggable
import fs2.Stream
import org.ergoplatform.common.cache.errors.{BinaryDecodingFailed, BinaryEncodingFailed}
import cats.syntax.either._
import scodec.bits.BitVector
import tofu.syntax.raise._

object CacheStreamingGenerator {

  def cacheStreamingFor(orders: List[CFMMOrder]): CacheStreaming[StreamF] = new CacheStreaming[StreamF] {

    override def getAll[V: Codec: Loggable]: StreamF[V] =
      Stream
        .emits[IO, CFMMOrder](orders)
        .evalMap(elem =>
          implicitly(Codec[CFMMOrder]
            .encode(elem)
            .toEither
            .leftMap(err => BinaryEncodingFailed(elem.toString, err.messageWithContext))
            .toRaise[IO])
        )
        .evalMap(bytes =>
          Codec[V]
            .decode(bytes)
            .toEither
            .map(_.value)
            .leftMap(err => BinaryDecodingFailed("testKey", err.messageWithContext))
            .toRaise[IO]
        )
  }
}
