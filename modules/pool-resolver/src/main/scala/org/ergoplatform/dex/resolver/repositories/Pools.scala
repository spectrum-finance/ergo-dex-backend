package org.ergoplatform.dex.resolver.repositories

import org.ergoplatform.dex.domain.amm.state.{Confirmed, Predicted}
import org.ergoplatform.dex.domain.amm.{CFMMPool, PoolId}

trait Pools[F[_]] {

  /** Get last predicted state of a pool with the given `id`.
    */
  def getLastPredicted(id: PoolId): F[Option[Predicted[CFMMPool]]]

  /** Get last confirmed state of a pool with the given `id`.
    */
  def getLastConfirmed(id: PoolId): F[Option[Confirmed[CFMMPool]]]

  /** Persist predicted pool.
    */
  def put(pool: Predicted[CFMMPool]): F[Unit]

  /** Persist confirmed pool state.
    */
  def put(pool: Confirmed[CFMMPool]): F[Unit]

  /** Check whether a pool state prediction with the given `id` exists.
    */
  def existsPredicted(id: PoolId): F[Boolean]
}
