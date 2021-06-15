package org.ergoplatform.dex.executor.amm.repositories

import cats.Monad
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

  final class Live[F[_]: Monad](implicit network: ErgoNetwork[F]) extends CFMMPools[F] {

    def get(id: PoolId): F[Option[CFMMPool]] = ???

    def put(pool: Predicted[CFMMPool]): F[Unit] = ???
  }
}
