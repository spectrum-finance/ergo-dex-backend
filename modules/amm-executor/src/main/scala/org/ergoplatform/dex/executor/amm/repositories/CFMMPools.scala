package org.ergoplatform.dex.executor.amm.repositories

import cats.Monad
import dev.profunktor.redis4cats.RedisCommands
import org.ergoplatform.dex.domain.amm.state.Predicted
import org.ergoplatform.dex.domain.amm.{CFMMPool, PoolId}
import org.ergoplatform.ergo.ErgoNetwork

trait CFMMPools[F[_]] {

  /** Get pool state by pool id.
    */
  def get(id: PoolId): F[Option[CFMMPool]]

  /** Persist predicted pool.
    */
  def put(pool: Predicted[CFMMPool]): F[Unit]
}

object CFMMPools {

  def make[F[_]: Monad](implicit network: ErgoNetwork[F]): CFMMPools[F] =
    new Live[F]

  final class Live[F[_]: Monad](implicit
    network: ErgoNetwork[F],
    redis: RedisCommands[F, String, String]
  ) extends CFMMPools[F] {

    /** predicted:poolId:boxId -> pool
      * onchain:poolId:boxId -> pool
     *
     * 1. Get(onchain:poolId:last) 2. Get(predicted:poolId:last)
     * -> (A. [2] is based on [1] => Use [2]),
     *    (B. [2] is behind [1]
     *      => (B.1. [1] belongs to [2] chain        => Update [2]; Use [2])
     *         (B.2. [1] doesn't belong to [2] chain => Use [1] ))
      */
    def get(id: PoolId): F[Option[CFMMPool]] = ???

    def put(pool: Predicted[CFMMPool]): F[Unit] = ???
  }
}
