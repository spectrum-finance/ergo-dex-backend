package org.ergoplatform.ergo.modules

import derevo.derive
import org.ergoplatform.ErgoLikeTransaction
import org.ergoplatform.ergo.TxId
import org.ergoplatform.ergo.domain.{EpochParams, NetworkInfo}
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
