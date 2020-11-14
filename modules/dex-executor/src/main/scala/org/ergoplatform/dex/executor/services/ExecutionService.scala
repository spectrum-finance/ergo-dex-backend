package org.ergoplatform.dex.executor.services

import cats.{Apply, Functor, Monad}
import derevo.derive
import org.ergoplatform.dex.clients.ErgoNetworkClient
import org.ergoplatform.dex.configs.ProtocolConfig
import org.ergoplatform.dex.domain.models.Trade.AnyTrade
import org.ergoplatform.dex.executor.config.ExchangeConfig
import org.ergoplatform.dex.executor.context.BlockchainContext
import org.ergoplatform.dex.executor.modules.Transactions
import tofu.higherKind.Mid
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.{Context, WithContext}
import tofu.syntax.monadic._
import tofu.syntax.logging._

@derive(representableK)
trait ExecutionService[F[_]] {

  /** Assembly Ergo transaction from a given `match`.
    */
  def execute(trade: AnyTrade): F[Unit]
}

object ExecutionService {

  def make[
    I[_]: Functor,
    F[_]: Monad: WithContext[*[_], ExchangeConfig]: WithContext[*[_], ProtocolConfig]
  ](implicit
    client: ErgoNetworkClient[F],
    logs: Logs[I, F]
  ): I[ExecutionService[F]] =
    logs.forService[ExecutionService[F]].map { implicit l =>
      new ExecutionServiceTracing[F] attach new ErgoToTokenExecutionService[F]
    }

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

  final private class ExecutionServiceTracing[F[_]: Apply: Logging] extends ExecutionService[Mid[F, *]] {

    def execute(trade: AnyTrade): Mid[F, Unit] =
      _ <* trace"Executing trade [$trade]"
  }
}
