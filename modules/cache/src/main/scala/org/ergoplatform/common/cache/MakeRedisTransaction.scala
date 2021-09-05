package org.ergoplatform.common.cache

import cats.Parallel
import cats.effect.{Concurrent, ContextShift, Resource, Timer}
import dev.profunktor.redis4cats.transactions.RedisTransaction
import org.ergoplatform.common.cache.Redis._
import tofu.logging.{Logging, Logs}

trait MakeRedisTransaction[F[_]] {

  def make: Resource[F, Redis.PlainTx[F]]
}

object MakeRedisTransaction {

  def make[F[_]: Parallel: Concurrent: ContextShift: Timer: RedisConfig.Has](implicit
    logs: Logs[F, F]
  ): MakeRedisTransaction[F] = new PlainCEInstance[F]

  final class PlainCEInstance[
    F[_]: Parallel: Concurrent: ContextShift: Timer: RedisConfig.Has
  ](implicit logs: Logs[F, F])
    extends MakeRedisTransaction[F] {

    def make: Resource[F, Redis.PlainTx[F]] =
      for {
        implicit0(log: Logging[F]) <- Resource.eval(logs.byName("redis-tx"))
        conf                       <- Resource.eval(RedisConfig.access)
        redis                      <- Redis.make[F, F](conf)
      } yield RedisTransaction(redis)
  }
}
