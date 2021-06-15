package org.ergoplatform.dex.tracker.repositories

import cats.Monad
import org.ergoplatform.dex.domain.{OnChain, Predicted}
import org.ergoplatform.dex.domain.amm.{CFMMPool, PoolId}
import org.ergoplatform.ergo.ErgoNetwork

trait CFMMPools[F[_]] {

  /** Get actual pool state by pool id.
    */
  def getOnChain(id: PoolId): F[Option[OnChain[CFMMPool]]]

  /** Put actual pool state.
    */
  def putOnChain(pool: OnChain[CFMMPool]): F[Unit]

  /** Get latest predicted pool state by pool id.
    */
  def getPredicted(id: PoolId): F[Option[Predicted[CFMMPool]]]

  /** Put predicted pool state.
    */
  def putPredicted(pool: Predicted[CFMMPool]): F[Unit]
}

object CFMMPools {

  def make[F[_]: Monad](implicit network: ErgoNetwork[F]): CFMMPools[F] =
    new Live[F]

  final class Live[F[_]: Monad](implicit network: ErgoNetwork[F]) extends CFMMPools[F] {

    def getOnChain(id: PoolId): F[Option[OnChain[CFMMPool]]] = ???

    def putOnChain(pool: OnChain[CFMMPool]): F[Unit] = ???

    def getPredicted(id: PoolId): F[Option[Predicted[CFMMPool]]] = ???

    def putPredicted(pool: Predicted[CFMMPool]): F[Unit] = ???
  }
}
