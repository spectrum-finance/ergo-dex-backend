package org.ergoplatform.dex.executor.amm.services

import cats.{Apply, Functor, Monad}
import derevo.derive
import org.ergoplatform.dex.clients.ErgoNetworkClient
import org.ergoplatform.dex.configs.ProtocolConfig
import org.ergoplatform.dex.domain.orderbook.Trade.AnyTrade
import org.ergoplatform.dex.protocol.instances._
import org.ergoplatform.dex.executor.amm.config.ExchangeConfig
import org.ergoplatform.dex.executor.amm.context.BlockchainContext
import org.ergoplatform.dex.executor.amm.domain.errors.ExecutionFailure
import org.ergoplatform.dex.executor.amm.modules.Transactions
import tofu.higherKind.Mid
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.Raise
import tofu.syntax.monadic._
import tofu.syntax.logging._
import tofu.syntax.context._

@derive(representableK)
trait ExecutionService[F[_]] {

  /** Assembly Ergo transaction from a given `match`.
    */
  def execute(trade: AnyTrade): F[Unit]
}

object ExecutionService {

  private val ValuePerByte = 360L // todo: fetch from explorer

  def make[
    I[_]: Functor,
    F[_]: Monad: Raise[*[_], ExecutionFailure]: ExchangeConfig.Has: ProtocolConfig.Has: BlockchainContext.Local
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
    F[_]: Monad: ExchangeConfig.Has: ProtocolConfig.Has: BlockchainContext.Local: Logging
  ](implicit
    client: ErgoNetworkClient[F],
    txs: Transactions[F]
  ) extends ExecutionService[F] {

    import io.circe.syntax._
    import org.ergoplatform.dex.protocol.codecs._

    def execute(trade: AnyTrade): F[Unit] =
      for {
        height <- client.getCurrentHeight
        tx     <- txs.translate(trade).local(_ => BlockchainContext(height, ValuePerByte))
        _      <- info"Transaction assembled $tx"
        _      <- debug"${tx.asJson.noSpacesSortKeys}"
        _      <- client.submitTransaction(tx)
      } yield () // todo: save and track tx id, retry if transaction failed.
  }

  final private class ExecutionServiceTracing[F[_]: Apply: Logging] extends ExecutionService[Mid[F, *]] {

    def execute(trade: AnyTrade): Mid[F, Unit] =
      _ <* trace"Executing trade [$trade]"
  }
}
