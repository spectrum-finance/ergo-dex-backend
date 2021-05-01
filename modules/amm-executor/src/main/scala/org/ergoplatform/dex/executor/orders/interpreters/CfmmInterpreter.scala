package org.ergoplatform.dex.executor.orders.interpreters

import org.ergoplatform.ErgoLikeTransaction
import org.ergoplatform.dex.domain.amm.{Deposit, Redeem, Swap}
import org.ergoplatform.dex.protocol.amm.AmmContractType.CfmmFamily

/** Interprets CFMM operations to a transaction
  */
trait CfmmInterpreter[CT <: CfmmFamily, F[_]] {

  def deposit(in: Deposit): F[ErgoLikeTransaction]

  def redeem(in: Redeem): F[ErgoLikeTransaction]

  def swap(in: Swap): F[ErgoLikeTransaction]
}
