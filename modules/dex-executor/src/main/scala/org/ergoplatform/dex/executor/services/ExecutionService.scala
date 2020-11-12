package org.ergoplatform.dex.executor.services

import cats.Monad
import org.ergoplatform.dex.clients.ErgoNetworkClient
import org.ergoplatform.dex.configs.ProtocolConfig
import org.ergoplatform.dex.domain.models.Trade.AnyTrade
import org.ergoplatform.dex.executor.config.ExchangeConfig
import org.ergoplatform.dex.executor.context.BlockchainContext
import org.ergoplatform.dex.executor.modules.Transactions
import tofu.{Context, WithContext}
import tofu.syntax.monadic._

abstract class ExecutionService[F[_]] {

  /** Assembly Ergo transaction from a given `match`.
    */
  def execute(trade: AnyTrade): F[Unit]
}

object ExecutionService {

  /** Implements processing of trades necessarily involving ERG.
    */
  final private class ErgoToTokenExecutionService[
    F[_]: Monad: WithContext[*[_], ExchangeConfig]: WithContext[*[_], ProtocolConfig]
  ](implicit
    client: ErgoNetworkClient[F]
  ) extends ExecutionService[F] {

    def execute(trade: AnyTrade): F[Unit] =
      (client.getCurrentHeight map BlockchainContext)
        .map(Context.const[F, BlockchainContext])
        .flatMap(implicit ctx => Transactions[F].translate(trade))
        .flatMap(client.submitTransaction)
  }
}
