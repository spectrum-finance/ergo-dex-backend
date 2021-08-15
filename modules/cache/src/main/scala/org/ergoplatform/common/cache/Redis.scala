package org.ergoplatform.common.cache

import cats.effect.{Concurrent, ContextShift, Resource}
import cats.tagless.syntax.functorK._
import dev.profunktor.redis4cats.RedisCommands._
import dev.profunktor.redis4cats.data.RedisCodec
import dev.profunktor.redis4cats.effect.Log
import dev.profunktor.redis4cats.transactions.RedisTransaction
import dev.profunktor.redis4cats.{RedisCommands, Redis => CERedis}
import tofu.lift.Lift
import tofu.logging.{Logging, Logs}

object Redis {

  type Plain[F[_]]   = RedisCommands[F, Array[Byte], Array[Byte]]
  type PlainTx[F[_]] = RedisTransaction[F, Array[Byte], Array[Byte]]

  /** Create new Redis client
    */
  def make[I[_]: Concurrent: ContextShift, F[_]: Concurrent: ContextShift](
    conf: RedisConfig
  )(implicit logs: Logs[I, I], lift: Lift[I, F]): Resource[I, Plain[F]] =
    for {
      implicit0(logI: Logging[I]) <- Resource.eval(logs.byName("redis-client"))
      implicit0(logF: Logging[F]) = logI.mapK(lift.liftF)
      cmd <- CERedis[I].simple(conf.uri, RedisCodec.Bytes).map(_.liftK[F])
    } yield cmd

  implicit def logInstance[F[_]](implicit logger: Logging[F]): Log[F] =
    new Log[F] {
      def debug(msg: => String): F[Unit] = logger.debug(msg)

      def error(msg: => String): F[Unit] = logger.error(msg)

      def info(msg: => String): F[Unit] = logger.info(msg)
    }
}
