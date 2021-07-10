package org.ergoplatform.common.cache

import cats.effect.{Concurrent, Timer}
import cats.{Functor, Parallel}
import dev.profunktor.redis4cats.effect.Log
import dev.profunktor.redis4cats.transactions.RedisTransaction
import org.ergoplatform.common.cache.Redis._
import tofu.logging.Logs
import tofu.syntax.monadic._

trait MakeRedisTransaction[F[_]] {

  def make: Redis.PlainTx[F]
}

object MakeRedisTransaction {

  def make[I[_]: Functor, F[_]: Parallel: Concurrent: Timer](implicit
    redis: Redis.Plain[F],
    logs: Logs[I, F]
  ): I[MakeRedisTransaction[F]] =
    logs.byName("redis-tx").map(implicit l => new PlainCEInstance[F])

  final class PlainCEInstance[F[_]: Parallel: Concurrent: Timer: Log](implicit redis: Redis.Plain[F])
    extends MakeRedisTransaction[F] {
    def make: Redis.PlainTx[F] = RedisTransaction(redis)
  }
}
