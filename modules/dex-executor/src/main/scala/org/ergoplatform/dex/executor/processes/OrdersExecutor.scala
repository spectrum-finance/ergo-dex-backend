package org.ergoplatform.dex.executor.processes

import cats.{Foldable, Functor, Monad}
import derevo.derive
import org.ergoplatform.dex.context.HasCommitPolicy
import org.ergoplatform.dex.domain.models.Trade.AnyTrade
import org.ergoplatform.dex.executor.services.ExecutionService
import org.ergoplatform.dex.streaming.syntax._
import org.ergoplatform.dex.streaming.{CommitPolicy, Consumer}
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.streams.{Evals, Temporal}
import tofu.syntax.context._
import tofu.syntax.embed._
import tofu.syntax.monadic._
import tofu.syntax.streams.evals._

@derive(representableK)
trait OrdersExecutor[F[_]] {

  def run: F[Unit]
}

object OrdersExecutor {

  def make[
    I[_]: Functor,
    F[_]: Monad: Evals[*[_], G]: Temporal[*[_], C]: HasCommitPolicy,
    G[_]: Monad,
    C[_]: Foldable
  ](implicit
    consumer: Consumer[String, AnyTrade, F, G],
    executor: ExecutionService[G],
    logs: Logs[I, G]): I[OrdersExecutor[F]] =
    logs.forService[OrdersExecutor[F]] map { implicit l =>
      (context[F] map (policy => new Live[F, G, C](policy): OrdersExecutor[F])).embed
    }

  final private class Live[
    F[_]: Monad: Evals[*[_], G]: Temporal[*[_], C],
    G[_]: Monad: Logging,
    C[_]: Foldable
  ](commitPolicy: CommitPolicy)(implicit
    consumer: Consumer[String, AnyTrade, F, G],
    executor: ExecutionService[G]
  ) extends OrdersExecutor[F] {

    def run: F[Unit] =
      consumer.stream
        .evalTap { rec =>
          val trade = rec.message
          executor.execute(trade)
        }
        .commitBatchWithin[C](commitPolicy.maxBatchSize, commitPolicy.commitTimeout)
  }
}
