package org.ergoplatform.dex.tracker.repositories

import cats.effect.{Concurrent, Timer}
import cats.{FlatMap, Functor, Parallel}
import derevo.derive
import org.ergoplatform.common.cache.{Cache, MakeRedisTransaction, Redis}
import scodec.Codec
import scodec.codecs._
import tofu.higherKind.Mid
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._

@derive(representableK)
trait TrackerCache[F[_]] {

  def lastScannedBoxOffset: F[Long]

  def lastScannedTxOffset: F[Long]

  def setLastScannedBoxOffset(n: Long): F[Unit]

  def setLastScannedTxOffset(n: Long): F[Unit]
}

object TrackerCache {

  def make[I[_]: FlatMap, F[_]: Parallel: Concurrent: Timer](implicit
    redis: Redis.Plain[F],
    makeTx: MakeRedisTransaction[F],
    logs: Logs[I, F]
  ): I[TrackerCache[F]] =
    for {
      implicit0(log: Logging[F]) <- logs.forService[TrackerCache[F]]
      cache                      <- Cache.make[I, F]
    } yield new CacheTracing[F] attach new Live[F](cache)

  private val LastScannedBoxOffsetKey = "last_scanned_box"
  private val LastScannedTxOffsetKey  = "last_scanned_tx"
  private val NullOffset              = -1L

  final class Live[F[_]: Functor](cache: Cache[F]) extends TrackerCache[F] {

    implicit val codecString: Codec[String] = utf8
    implicit val codecLong: Codec[Long]     = int64

    def lastScannedBoxOffset: F[Long] =
      cache.get[String, Long](LastScannedBoxOffsetKey).map(_.getOrElse(NullOffset))

    def lastScannedTxOffset: F[Long] =
      cache.get[String, Long](LastScannedTxOffsetKey).map(_.getOrElse(NullOffset))

    def setLastScannedBoxOffset(n: Long): F[Unit] =
      cache.set(LastScannedBoxOffsetKey, n)

    def setLastScannedTxOffset(n: Long): F[Unit] =
      cache.set(LastScannedTxOffsetKey, n)
  }

  final class CacheTracing[F[_]: FlatMap: Logging] extends TrackerCache[Mid[F, *]] {

    def lastScannedBoxOffset: Mid[F, Long] =
      _ >>= (r => trace"lastScannedBoxOffset = $r" as r)

    def lastScannedTxOffset: Mid[F, Long] =
      _ >>= (r => trace"lastScannedTxOffset = $r" as r)

    def setLastScannedBoxOffset(n: Long): Mid[F, Unit] =
      trace"setLastScannedBoxOffset(n=$n)" *> _

    def setLastScannedTxOffset(n: Long): Mid[F, Unit] =
      trace"setLastScannedTxOffset(n=$n)" *> _
  }
}
