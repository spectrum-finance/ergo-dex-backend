package org.ergoplatform.ergo.modules

import derevo.derive
import org.ergoplatform.ErgoLikeTransaction
import org.ergoplatform.ergo.TxId
import org.ergoplatform.ergo.domain.{EpochParams, NetworkInfo}
import org.ergoplatform.ergo.services.explorer.ErgoExplorer
import org.ergoplatform.ergo.services.node.ErgoNode
import tofu.higherKind.derived.representableK

@derive(representableK)
trait ErgoNetwork[F[_]] {

  /** Submit a transaction to the network.
    */
  def submitTransaction(tx: ErgoLikeTransaction): F[TxId]

  /** Check a given transaction.
    * @return None if transaction is valid, Some(error_description) otherwise.
    */
  def checkTransaction(tx: ErgoLikeTransaction): F[Option[String]]

  /** Get current network height.
    */
  def getCurrentHeight: F[Int]

  /** Get latest epoch params.
    */
  def getEpochParams: F[EpochParams]

  /** Get latest network info.
    */
  def getNetworkInfo: F[NetworkInfo]
}

object ErgoNetwork {

  def make[F[_]](implicit explorer: ErgoExplorer[F], node: ErgoNode[F]): ErgoNetwork[F] =
    new CombinedErgoNetwork(explorer, node)

  final class CombinedErgoNetwork[F[_]](explorer: ErgoExplorer[F], node: ErgoNode[F]) extends ErgoNetwork[F] {
    def submitTransaction(tx: ErgoLikeTransaction): F[TxId]          = node.submitTransaction(tx)
    def checkTransaction(tx: ErgoLikeTransaction): F[Option[String]] = explorer.checkTransaction(tx)
    def getCurrentHeight: F[Int]                                     = explorer.getCurrentHeight
    def getEpochParams: F[EpochParams]                               = explorer.getEpochParams
    def getNetworkInfo: F[NetworkInfo]                               = explorer.getNetworkInfo
  }
}
