package org.ergoplatform.dex.clients

import org.ergoplatform.ErgoLikeTransaction

abstract class ErgoNetworkClient[F[_]] {

  /** Submit a transaction to the network.
    */
  def submitTransaction(tx: ErgoLikeTransaction): F[Unit]

  /** Get current network height.
    */
  def getCurrentHeight: F[Int]
}
