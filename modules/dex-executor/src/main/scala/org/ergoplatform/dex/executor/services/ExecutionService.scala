package org.ergoplatform.dex.executor.services

import cats.Monad
import org.ergoplatform.dex.clients.ErgoNetworkClient
import org.ergoplatform.dex.domain.models.Trade.AnyTrade
import org.ergoplatform.dex.executor.context.TxContext
import tofu.syntax.monadic._

abstract class ExecutionService[F[_]] {

  /** Assembly Ergo transaction from a given `match`.
    */
  def execute(anyMatch: AnyTrade): F[Unit]
}

object ExecutionService {

  /** Implements processing of trades necessarily involving ERG.
    */
  final private class ErgoToTokenExecutionService[F[_]: Monad](implicit
    client: ErgoNetworkClient[F],
    txs: Transactions[F]
  ) extends ExecutionService[F] {

    def execute(trade: AnyTrade): F[Unit] =
      (client.getCurrentHeight map TxContext)
        .flatMap(implicit ctx => txs.toTransaction(trade))
        .flatMap(client.submitTransaction)
  }
}
