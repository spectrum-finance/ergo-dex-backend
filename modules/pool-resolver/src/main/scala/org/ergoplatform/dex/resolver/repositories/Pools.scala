package org.ergoplatform.dex.resolver.repositories

import org.ergoplatform.dex.domain.amm.CFMMPool
import org.ergoplatform.dex.domain.amm.state.{OnChain, Predicted}

trait Pools[F[_]] {

  /** Persist predicted pool.
   */
  def put(pool: Predicted[CFMMPool]): F[Unit]

  /** Persist on-chain pool.
   */
  def put(pool: OnChain[CFMMPool]): F[Unit]
}
