package org.ergoplatform.dex.resolver.services

import org.ergoplatform.dex.domain.amm.{CFMMPool, PoolId}

trait Resolver[F[_]] {

  /** Get pool state by pool id.
    */
  def resolve(id: PoolId): F[Option[CFMMPool]]
}

object Resolver {

  final class Live[F[_]] extends Resolver[F] {

    /** predicted:poolId:boxId -> pool
     * onchain:poolId:boxId -> pool
     *
     * 1. Get(onchain:poolId:last) 2. Get(predicted:poolId:last)
     * -> (A. [2] is based on [1] => Use [2]),
     *    (B. [2] is behind [1]
     *      => (B.1. [2] is successor of [1]    => Update [2] -> GIX[2] = GIX[1]; Use [2])
     *         (B.2. [2] isn't successor of [1] => Use [1]; Update [2] -> [1] ))
     */
    def resolve(id: PoolId): F[Option[CFMMPool]] = ???
  }
}
