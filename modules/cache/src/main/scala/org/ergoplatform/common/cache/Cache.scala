package org.ergoplatform.common.cache

import cats.data.OptionT
import cats.syntax.either._
import cats.syntax.show._
import cats.{Monad, Show}
import org.ergoplatform.common.cache.errors.{BinaryDecodingFailed, BinaryEncodingFailed}
import scodec.Codec
import scodec.bits.BitVector
import tofu.Throws
import tofu.syntax.monadic._
import tofu.syntax.raise._

trait Cache[F[_]] {

  def set[K: Codec: Show, V: Codec: Show](key: K, value: V): F[Unit]

  def get[K: Codec: Show, V: Codec: Show](key: K): F[Option[V]]
}

object Cache {

  def make[F[_]: Monad: Throws](implicit redis: Redis.Plain[F]): Cache[F] =
    new Live[F](redis)

  final class Live[
    F[_]: Monad: BinaryEncodingFailed.Raise: BinaryDecodingFailed.Raise
  ](redis: Redis.Plain[F])
    extends Cache[F] {

    def set[K: Codec: Show, V: Codec: Show](key: K, value: V): F[Unit] =
      for {
        k <- Codec[K]
               .encode(key)
               .toEither
               .leftMap(err => BinaryEncodingFailed(key.show, err.messageWithContext))
               .toRaise
        v <- Codec[V]
               .encode(value)
               .toEither
               .leftMap(err => BinaryEncodingFailed(key.show, err.messageWithContext))
               .toRaise
        _ <- redis.set(k.toByteArray, v.toByteArray)
      } yield ()

    def get[K: Codec: Show, V: Codec: Show](key: K): F[Option[V]] =
      (for {
        k <- OptionT.liftF(
               Codec[K]
                 .encode(key)
                 .toEither
                 .leftMap(err => BinaryEncodingFailed(key.show, err.messageWithContext))
                 .toRaise
             )
        raw <- OptionT(redis.get(k.toByteArray))
        value <- OptionT.liftF(
                   Codec[V]
                     .decode(BitVector(raw))
                     .toEither
                     .map(_.value)
                     .leftMap(err => BinaryDecodingFailed(key.show, err.messageWithContext))
                     .toRaise
                 )
      } yield value).value
  }
}
