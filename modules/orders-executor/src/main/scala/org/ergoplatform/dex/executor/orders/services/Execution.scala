package org.ergoplatform.dex.executor.orders.services

import cats.{Apply, Functor, Monad}
import derevo.derive
import org.ergoplatform.dex.configs.ProtocolConfig
import org.ergoplatform.dex.domain.orderbook.Trade.AnyTrade
import org.ergoplatform.dex.executor.orders.config.ExchangeConfig
import org.ergoplatform.dex.executor.orders.context.BlockchainContext
import org.ergoplatform.dex.executor.orders.domain.errors.ExecutionFailure
import org.ergoplatform.dex.executor.orders.modules.TradeInterpreter
import org.ergoplatform.dex.protocol.instances._
import org.ergoplatform.ergo.ErgoNetwork
import tofu.Raise
import tofu.higherKind.Mid
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.syntax.context._
import tofu.syntax.logging._
import tofu.syntax.monadic._

@derive(representableK)
trait Execution[F[_]] {

  /** Execute a given trade.
    */
  def execute(trade: AnyTrade): F[Unit]
}

object Execution {

  private val ValuePerByte = 360L // todo: fetch from explorer

  def make[
    I[_]: Functor,
    F[_]: Monad: Raise[*[_], ExecutionFailure]: ExchangeConfig.Has: ProtocolConfig.Has: BlockchainContext.Local
  ](implicit
    client: ErgoNetwork[F],
    logs: Logs[I, F]
  ): I[Execution[F]] =
    logs.forService[Execution[F]].map { implicit l =>
      new ExecutionTracing[F] attach new ErgoToTokenExecution[F]
    }

  /** Implements processing of trades necessarily involving ERG.
    */
  final private class ErgoToTokenExecution[
    F[_]: Monad: ExchangeConfig.Has: ProtocolConfig.Has: BlockchainContext.Local: Logging
  ](implicit
    client: ErgoNetwork[F],
    interpreter: TradeInterpreter[F]
  ) extends Execution[F] {

    import io.circe.syntax._
    import org.ergoplatform.dex.protocol.codecs._

    def execute(trade: AnyTrade): F[Unit] =
      for {
        height <- client.getCurrentHeight
        tx     <- interpreter.trade(trade).local(_ => BlockchainContext(height, ValuePerByte))
        _      <- info"Transaction assembled $tx"
        _      <- debug"${tx.asJson.noSpacesSortKeys}"
        _      <- client.submitTransaction(tx)
      } yield () // todo: save and track tx id, retry if transaction failed.
  }

  final private class ExecutionTracing[F[_]: Apply: Logging] extends Execution[Mid[F, *]] {

    def execute(trade: AnyTrade): Mid[F, Unit] =
      _ <* trace"Executing trade [$trade]"
  }
}
