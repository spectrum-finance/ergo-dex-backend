package org.ergoplatform.dex.executor.modules

import cats.{Functor, Monad}
import fs2.Stream
import org.ergoplatform.dex.clients.ErgoNetworkClient
import org.ergoplatform.dex.domain.models.Trade.AnyTrade
import org.ergoplatform.dex.executor.context.TxContext
import org.ergoplatform.dex.executor.services.TransactionService
import org.ergoplatform.dex.streaming.Consumer
import tofu.logging.{Logging, Logs}
import tofu.syntax.monadic._
import tofu.syntax.logging._

abstract class OrdersExecutor[F[_]] {
  def run: Stream[F, Unit]
}

object OrdersExecutor {

  def make[I[_]: Functor, F[_]: Monad](
    consumer: Consumer[F, AnyTrade],
    txs: TransactionService[F],
    client: ErgoNetworkClient[F]
  )(implicit logs: Logs[I, F]): I[OrdersExecutor[F]] =
    logs.forService[OrdersExecutor[F]] map { implicit l =>
      new Live[F](consumer, txs, client)
    }

  final private class Live[F[_]: Monad: Logging](
    consumer: Consumer[F, AnyTrade],
    txs: TransactionService[F],
    client: ErgoNetworkClient[F]
  ) extends OrdersExecutor[F] {

    def run: Stream[F, Unit] =
      consumer.consume { rec =>
        Stream.emit(rec).covary[F].unNone >>= { mc =>
          Stream.eval(info"Executing $mc") >>
          Stream.eval(makeTxContext >>= (txs.makeTx(mc)(_)) >>= client.submitTransaction)
        }
      }

    private def makeTxContext: F[TxContext] =
      client.getCurrentHeight map TxContext
  }
}
