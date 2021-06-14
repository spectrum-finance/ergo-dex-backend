package org.ergoplatform.dex.tracker.repositories

import cats.Monad
import org.ergoplatform.dex.domain.{OnChain, Predicted}
import org.ergoplatform.dex.domain.amm.{CfmmPool, PoolId}
import org.ergoplatform.ergo.ErgoNetwork

trait CfmmPools[F[_]] {

  /** Get actual pool state by pool id.
    */
  def getOnChain(id: PoolId): F[Option[OnChain[CfmmPool]]]

  /** Put actual pool state.
    */
  def putOnChain(pool: OnChain[CfmmPool]): F[Unit]

  /** Get latest predicted pool state by pool id.
    */
  def getPredicted(id: PoolId): F[Option[Predicted[CfmmPool]]]

  /** Put predicted pool state.
    */
  def putPredicted(pool: Predicted[CfmmPool]): F[Unit]
}

object CfmmPools {

  def make[F[_]: Monad](implicit network: ErgoNetwork[F]): CfmmPools[F] =
    new Live[F]

  final class Live[F[_]: Monad](implicit network: ErgoNetwork[F]) extends CfmmPools[F] {

    def getOnChain(id: PoolId): F[Option[OnChain[CfmmPool]]] = ???

    def putOnChain(pool: OnChain[CfmmPool]): F[Unit] = ???

    def getPredicted(id: PoolId): F[Option[Predicted[CfmmPool]]] = ???

    def putPredicted(pool: Predicted[CfmmPool]): F[Unit] = ???
  }
}
