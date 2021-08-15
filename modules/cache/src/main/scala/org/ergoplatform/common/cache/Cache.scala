package org.ergoplatform.common.cache

import cats.data.OptionT
import cats.syntax.either._
import cats.syntax.show._
import cats.{Functor, Monad, Show}
import derevo.derive
import derevo.tagless.applyK
import dev.profunktor.redis4cats.hlist.{HList, Witness}
import org.ergoplatform.common.cache.errors.{BinaryDecodingFailed, BinaryEncodingFailed}
import scodec.Codec
import scodec.bits.BitVector
import tofu.Throws
import tofu.higherKind.Mid
import tofu.logging.{Loggable, Logging, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.raise._

@derive(applyK)
trait Cache[F[_]] {

  def set[K: Codec: Loggable, V: Codec: Loggable](key: K, value: V): F[Unit]

  def get[K: Codec: Loggable, V: Codec: Loggable](key: K): F[Option[V]]

  def del[K: Codec: Loggable](key: K): F[Unit]

  def transaction[T <: HList](commands: T)(implicit w: Witness[T]): F[Unit]
}

object Cache {

  def make[I[_]: Functor, F[_]: Monad: Throws](implicit
    redis: Redis.Plain[F],
    makeTx: MakeRedisTransaction[F],
    logs: Logs[I, F]
  ): I[Cache[F]] =
    logs.forService[Cache[F]].map { implicit l =>
      new CacheTracing[F] attach new Live[F]
    }

  final class Live[
    F[_]: Monad: BinaryEncodingFailed.Raise: BinaryDecodingFailed.Raise
  ](implicit redis: Redis.Plain[F], makeTx: MakeRedisTransaction[F])
    extends Cache[F] {

    implicit def showFromLoggable[T](implicit l: Loggable[T]): Show[T] = l.showInstance

    def set[K: Codec: Loggable, V: Codec: Loggable](key: K, value: V): F[Unit] =
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

    def get[K: Codec: Loggable, V: Codec: Loggable](key: K): F[Option[V]] =
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

    def del[K: Codec: Loggable](key: K): F[Unit] =
      Codec[K]
        .encode(key)
        .toEither
        .leftMap(err => BinaryEncodingFailed(key.show, err.messageWithContext))
        .toRaise
        .flatMap(k => redis.del(k.toByteArray).void)

    def transaction[T <: HList](commands: T)(implicit w: Witness[T]): F[Unit] =
      makeTx.make.exec(commands).void
  }

  final class CacheTracing[F[_]: Monad: Logging] extends Cache[Mid[F, *]] {

    def set[K: Codec: Loggable, V: Codec: Loggable](key: K, value: V): Mid[F, Unit] =
      _ <* trace"set(key=$key, value=$value) -> ()"

    def get[K: Codec: Loggable, V: Codec: Loggable](key: K): Mid[F, Option[V]] =
      _ >>= (r => trace"get(key=$key) -> $r" as r)

    def del[K: Codec: Loggable](key: K): Mid[F, Unit] =
      _ <* trace"del(key=$key) -> ()"

    def transaction[T <: HList](commands: T)(implicit w: Witness[T]): Mid[F, Unit] =
      fa => trace"transaction begin" >> fa.flatTap(_ => trace"transaction end")
  }
}
