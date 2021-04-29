package org.ergoplatform.dex.executor.amm.processes

import cats.{Foldable, Functor, Monad}
import derevo.derive
import mouse.any._
import org.ergoplatform.dex.executor.amm.domain.errors.ExecutionFailure
import org.ergoplatform.dex.executor.amm.streaming.StreamingBundle
import org.ergoplatform.dex.streaming.CommitPolicy
import tofu.Handle
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.streams.{Evals, Temporal}
import tofu.syntax.context._
import tofu.syntax.embed._
import tofu.syntax.monadic._

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
  ](implicit streaming: StreamingBundle[F, G], logs: Logs[I, G]): I[OrdersExecutor[F]] =
    logs.forService[OrdersExecutor[F]] map { implicit l =>
      (context[F] map (policy => new Live[F, G, C](policy): OrdersExecutor[F])).embed
    }

  final private class Live[
    F[_]: Monad: Evals[*[_], G]: Temporal[*[_], C]: Handle[*[_], ExecutionFailure],
    G[_]: Monad: Logging,
    C[_]: Foldable
  ](commitPolicy: CommitPolicy)(implicit
    streaming: StreamingBundle[F, G]
  ) extends OrdersExecutor[F] {

    def run: F[Unit] = ???
  }
}
