package org.ergoplatform.dex.tracker.repositories

import cats.{Functor, Monad}
import org.ergoplatform.common.cache.{Cache, Redis}
import scodec.Codec
import scodec.codecs._
import tofu.Throws
import tofu.syntax.monadic._

trait TrackerCache[F[_]] {

  def lastScannedBoxOffset: F[Long]

  def setLastScannedBoxOffset(n: Long): F[Unit]
}

object TrackerCache {

  def make[F[_]: Monad: Throws](implicit redis: Redis.Plain[F]): TrackerCache[F] =
    new Live[F](Cache.make)

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
}
