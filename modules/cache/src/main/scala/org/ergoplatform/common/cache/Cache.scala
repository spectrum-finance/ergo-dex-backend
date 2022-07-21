package org.ergoplatform.common.cache

import cats.data.OptionT
import cats.syntax.either._
import cats.syntax.traverse._
import cats.syntax.show._
import cats.{Functor, Monad, MonadError, Show}
import derevo.derive
import derevo.tagless.applyK
import dev.profunktor.redis4cats.data.KeyScanCursor
import dev.profunktor.redis4cats.hlist.{HList, Witness}
import org.ergoplatform.common.cache.errors.{BinaryDecodingFailed, BinaryEncodingFailed, ValueNotFound}
import scodec.Codec
import scodec.bits.BitVector
import tofu.BracketThrow
import tofu.higherKind.Mid
import tofu.logging.{Loggable, Logging, Logs}
import tofu.syntax.logging._
import tofu.syntax.loggable._
import tofu.syntax.monadic._
import tofu.syntax.raise._
import fs2.Stream

@derive(applyK)
trait Cache[F[_]] {

  def set[K: Codec: Loggable, V: Codec: Loggable](key: K, value: V): F[Unit]

  def get[K: Codec: Loggable, V: Codec: Loggable](key: K): F[Option[V]]

  def del[K: Codec: Loggable](key: K): F[Unit]

  def exists[K: Codec: Loggable](key: K): F[Boolean]

  def getAll[V: Codec: Loggable]: F[List[V]]

  def flushAll: F[Unit]

  def transaction[T <: HList](commands: T)(implicit w: Witness[T]): F[Unit]
}

object Cache {

  implicit val loggable: Loggable[Array[Byte]] = Loggable.empty

  def make[I[_]: Functor, F[_]: Monad: BracketThrow](implicit
    redis: Redis.Plain[F],
    makeTx: MakeRedisTransaction[F],
    logs: Logs[I, F]
  ): I[Cache[F]] =
    logs.forService[Cache[F]].map { implicit l =>
      new CacheTracing[F] attach new Redis[F]
    }

  final class Redis[
    F[_]: Monad: BinaryEncodingFailed.Raise: BinaryDecodingFailed.Raise: ValueNotFound.Raise: BracketThrow
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
        value <- getValue[V](k.toByteArray)
      } yield value).value

    def del[K: Codec: Loggable](key: K): F[Unit] =
      Codec[K]
        .encode(key)
        .toEither
        .leftMap(err => BinaryEncodingFailed(key.show, err.messageWithContext))
        .toRaise
        .flatMap(k => redis.del(k.toByteArray).void)

    def flushAll: F[Unit] =
      redis.flushAll

    def exists[K: Codec: Loggable](key: K): F[Boolean] =
      Codec[K]
        .encode(key)
        .toEither
        .leftMap(err => BinaryEncodingFailed(key.show, err.messageWithContext))
        .toRaise
        .flatMap(k => redis.exists(k.toByteArray))

    def transaction[T <: HList](commands: T)(implicit w: Witness[T]): F[Unit] =
      makeTx.make.use(_.exec(commands).void)

    def getAll[V: Codec: Loggable]: F[List[V]] = {
      def iterate(acc: List[V], scanner: KeyScanCursor[Array[Byte]]): F[List[V]] =
        for {
          elems <- scanner.keys
                     .traverse(key =>
                       getValue[V](key).value >>= {
                         case Some(elem) => elem.pure
                         case None       => ValueNotFound(key.logShow).raise[F, V]
                       }
                     )
          newAcc = acc ++ elems
          toReturn <-
            if (scanner.isFinished) newAcc.pure
            else redis.scan(scanner) >>= (iterate(newAcc, _))
        } yield toReturn

      redis.scan >>= (iterate(List.empty, _))
    }

    private def getValue[V: Codec: Loggable](key: Array[Byte]) = for {
      raw <- OptionT(redis.get(key))
      value <- OptionT.liftF(
        Codec[V]
          .decode(BitVector(raw))
          .toEither
          .map(_.value)
          .leftMap(err => BinaryDecodingFailed(key.show, err.messageWithContext))
          .toRaise
      )
    } yield value

    def getAllStream[V: Codec : Loggable]: Stream[F, V] = {
      def iterate(scanner: KeyScanCursor[Array[Byte]]): Stream[F, V] =
        Stream.evals(
          scanner.keys
            .traverse(key =>
              getValue[V](key).value >>= {
                case Some(elem) => elem.pure
                case None       => ValueNotFound(key.logShow).raise[F, V]
              }
            )
        ) ++ (
          if (scanner.isFinished) Stream.empty
          else Stream.eval(redis.scan(scanner)) >>= iterate
        )

      Stream.eval(redis.scan) >>= iterate
    }

  }

  final class CacheTracing[F[_]: Monad: Logging] extends Cache[Mid[F, *]] {

    def set[K: Codec: Loggable, V: Codec: Loggable](key: K, value: V): Mid[F, Unit] =
      _ <* trace"set(key=$key, value=$value) -> ()"

    def get[K: Codec: Loggable, V: Codec: Loggable](key: K): Mid[F, Option[V]] =
      _ >>= (r => trace"get(key=$key) -> $r" as r)

    def del[K: Codec: Loggable](key: K): Mid[F, Unit] =
      _ <* trace"del(key=$key) -> ()"

    def flushAll: Mid[F, Unit] =
      _ <* trace"flushAll -> ()"

    def transaction[T <: HList](commands: T)(implicit w: Witness[T]): Mid[F, Unit] =
      fa => trace"transaction begin" >> fa.flatTap(_ => trace"transaction end")

    def exists[K: Codec: Loggable](key: K): Mid[F, Boolean] =
      _ >>= (r => trace"exists(key=$key) -> $r" as r)

    def getAll[V: Codec: Loggable]: Mid[F, List[V]] =
      _ >>= (r => trace"getAll() -> length: ${r.length}" as r)

    def getAllStream[V: Codec : Loggable]: Stream[Mid[F, *], V] =
      Stream.eval(
        (fa: F[V]) => trace"getAllStream()" >> fa
      )
  }
}
