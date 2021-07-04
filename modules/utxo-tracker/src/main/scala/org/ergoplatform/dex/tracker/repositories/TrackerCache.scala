package org.ergoplatform.dex.tracker.repositories

import cats.{FlatMap, Functor, Monad}
import derevo.derive
import org.ergoplatform.common.cache.{Cache, Redis}
import scodec.Codec
import scodec.codecs._
import tofu.Throws
import tofu.higherKind.Mid
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._

@derive(representableK)
trait TrackerCache[F[_]] {

  def lastScannedBoxOffset: F[Long]

  def setLastScannedBoxOffset(n: Long): F[Unit]
}

object TrackerCache {

  def make[I[_]: Functor, F[_]: Monad: Throws](implicit redis: Redis.Plain[F], logs: Logs[I, F]): I[TrackerCache[F]] =
    logs.forService[TrackerCache[F]] map (implicit l => new CacheTracing[F] attach new Live[F](Cache.make))

  private val LastScannedBoxOffsetKey = "last_scanned_box"
  private val NullOffset              = -1L

  final class Live[F[_]: Functor](cache: Cache[F]) extends TrackerCache[F] {

    implicit val codecString: Codec[String] = utf8
    implicit val codecLong: Codec[Long]     = int64

    def lastScannedBoxOffset: F[Long] =
      cache.get[String, Long](LastScannedBoxOffsetKey).map(_.getOrElse(NullOffset))

    def setLastScannedBoxOffset(n: Long): F[Unit] =
      cache.set(LastScannedBoxOffsetKey, n)
  }

  final class CacheTracing[F[_]: FlatMap: Logging] extends TrackerCache[Mid[F, *]] {

    def lastScannedBoxOffset: Mid[F, Long] =
      _ >>= (r => trace"lastScannedBoxOffset = $r" as r)

    def setLastScannedBoxOffset(n: Long): Mid[F, Unit] =
      trace"setLastScannedBoxOffset(n=$n)" *> _
  }
}
