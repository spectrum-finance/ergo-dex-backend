package org.ergoplatform.dex.executor.services

import org.ergoplatform.ErgoLikeTransaction
import org.ergoplatform.dex.domain.models.Match.AnyMatch

abstract class TransactionService[F[_]] {

  /** Assembly Ergo transaction from a given `match`.
    */
  def makeTx(anyMatch: AnyMatch): F[ErgoLikeTransaction]
}
