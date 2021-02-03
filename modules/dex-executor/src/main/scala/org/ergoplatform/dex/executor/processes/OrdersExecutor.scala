package org.ergoplatform.dex.executor.processes

import cats.{Foldable, Functor, Monad}
import derevo.derive
import org.ergoplatform.dex.OrderId
import org.ergoplatform.dex.executor.domain.errors.ExecutionFailure
import org.ergoplatform.dex.executor.services.ExecutionService
import org.ergoplatform.dex.executor.streaming.StreamingBundle
import org.ergoplatform.dex.streaming.syntax._
import org.ergoplatform.dex.streaming.{CommitPolicy, Record}
import mouse.any._
import org.ergoplatform.dex.domain.models.Order.AnyOrder
import tofu.Handle
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.streams.{Evals, Temporal}
import tofu.syntax.context._
import tofu.syntax.embed._
import tofu.syntax.handle._
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.streams.all._

@derive(representableK)
trait OrdersExecutor[F[_]] {

  def run: F[Unit]
}

object OrdersExecutor {

  def make[
    I[_]: Functor,
    F[_]: Monad: Evals[*[_], G]: Temporal[*[_], C]: CommitPolicy.Has: Handle[*[_], ExecutionFailure],
    G[_]: Monad,
    C[_]: Foldable
  ](implicit streaming: StreamingBundle[F, G], executor: ExecutionService[G], logs: Logs[I, G]): I[OrdersExecutor[F]] =
    logs.forService[OrdersExecutor[F]] map { implicit l =>
      (context[F] map (policy => new Live[F, G, C](policy): OrdersExecutor[F])).embed
    }

  final private class Live[
    F[_]: Monad: Evals[*[_], G]: Temporal[*[_], C]: Handle[*[_], ExecutionFailure],
    G[_]: Monad: Logging,
    C[_]: Foldable
  ](commitPolicy: CommitPolicy)(implicit
    streaming: StreamingBundle[F, G],
    executor: ExecutionService[G]
  ) extends OrdersExecutor[F] {

    def run: F[Unit] =
      streaming.consumer.stream
        .flatTap { rec =>
          val trade = rec.message
          eval(executor.execute(trade))
            .handleWith[ExecutionFailure] { e =>
              val rotateOrders = trade.orders.map(o => Record[OrderId, AnyOrder](o.base.id, o.base))
              eval(warnCause"Trade [${trade.id}] execution failed. $trade" (e)) >>
              emits(rotateOrders).thrush(streaming.producer.produce)
            }
        }
        .commitBatchWithin[C](commitPolicy.maxBatchSize, commitPolicy.commitTimeout)
  }
}
