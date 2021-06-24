package org.ergoplatform.dex.executor.amm.processes

import cats.{Foldable, Functor, Monad}
import derevo.derive
import mouse.any._
import org.ergoplatform.common.streaming.CommitPolicy
import org.ergoplatform.dex.executor.amm.domain.errors.ExecutionFailed
import org.ergoplatform.dex.executor.amm.streaming.CFMMConsumer
import tofu.Handle
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.streams.{Evals, Temporal}
import tofu.syntax.context._
import tofu.syntax.embed._
import tofu.syntax.monadic._
import tofu.syntax.streams.all._
import org.ergoplatform.common.streaming.syntax._
import org.ergoplatform.dex.executor.amm.services.Execution

@derive(representableK)
trait Executor[F[_]] {

  def run: F[Unit]
}

object Executor {

  def make[
    I[_]: Functor,
    F[_]: Monad: Evals[*[_], G]: Temporal[*[_], C]: CommitPolicy.Has: Handle[*[_], ExecutionFailed],
    G[_]: Monad,
    C[_]: Foldable
  ](implicit
    consumer: CFMMConsumer[F, G],
    service: Execution[G],
    logs: Logs[I, G]
  ): I[Executor[F]] =
    logs.forService[Executor[F]] map { implicit l =>
      (context[F] map (policy => new Live[F, G, C](policy): Executor[F])).embed
    }

  final private class Live[
    F[_]: Monad: Evals[*[_], G]: Temporal[*[_], C]: Handle[*[_], ExecutionFailed],
    G[_]: Monad: Logging,
    C[_]: Foldable
  ](commitPolicy: CommitPolicy)(implicit
    consumer: CFMMConsumer[F, G],
    service: Execution[G]
  ) extends Executor[F] {

    def run: F[Unit] =
      consumer.stream
        .evalTap(rec => service.execute(rec.message))
        .commitBatchWithin[C](commitPolicy.maxBatchSize, commitPolicy.commitTimeout)
  }
}
