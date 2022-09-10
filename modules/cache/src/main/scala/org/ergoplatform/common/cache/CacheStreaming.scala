package org.ergoplatform.common.cache

import cats.{FlatMap, Monad, Show}
import cats.data.OptionT
import dev.profunktor.redis4cats.data.KeyScanCursor
import org.ergoplatform.common.cache.errors.{BinaryDecodingFailed, ValueNotFound}
import scodec.Codec
import derevo.tagless.applyK
import tofu.logging.{Loggable, Logging, Logs}
import cats.syntax.show._
import cats.syntax.traverse._
import cats.syntax.either._
import derevo.derive
import tofu.syntax.monadic._
import scodec.bits.BitVector
import tofu.BracketThrow
import tofu.higherKind.Mid
import tofu.higherKind.derived.representableK
import tofu.streams.{Evals, Merge}
import tofu.syntax.raise._
import tofu.syntax.loggable._
import tofu.syntax.logging._
import tofu.syntax.streams.evals._
import tofu.syntax.streams.merge._

@derive(representableK)
trait CacheStreaming[S[_]] {

  def getAll[V: Codec: Loggable]: S[V]
}

object CacheStreaming {

  implicit val loggable: Loggable[Array[Byte]] = Loggable[String].contramap(new String(_))

  def make[
    I[_]: FlatMap,
    F[_]: Evals[*[_], G]: Merge: Monad,
    G[_]: ValueNotFound.Raise: BracketThrow
  ](implicit
    redis: Redis.Plain[G],
    makeTx: MakeRedisTransaction[G],
    logs: Logs[I, G]
  ): I[CacheStreaming[F]] =
    logs.forService[CacheStreaming[F]].map { implicit logging =>
      new Live[F, G]
    }

  final private class Live[
    F[_]: Evals[*[_], G]: Merge: FlatMap,
    G[_]: ValueNotFound.Raise: BracketThrow
  ](implicit
    redis: Redis.Plain[G],
    makeTx: MakeRedisTransaction[G],
    logging: Logging[G]
  ) extends CacheStreaming[F] {

    implicit def showFromLoggable[T](implicit l: Loggable[T]): Show[T] = l.showInstance

    def getAll[V: Codec: Loggable]: F[V] = {

      def iterate(scanner: KeyScanCursor[Array[Byte]]): F[V] = {
        val elems = evals(
          scanner.keys
            .traverse(key =>
              getValue[V](key).value >>= {
                case Some(elem) =>
                  debug"Restore elem $key -> $elem" >> elem.pure
                case None => ValueNotFound(key.logShow).raise[G, V]
              }
            )
        )
        if (scanner.isFinished) elems
        else elems.merge(eval(redis.scan) >>= iterate)
      }

      eval(redis.scan) >>= iterate
    }

    private def getValue[V: Codec: Loggable](key: Array[Byte]): OptionT[G, V] = for {
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
  }
}
