package org.ergoplatform.dex.executor.amm.processes

import cats.effect.Clock
import cats.syntax.option._
import cats.{Defer, Functor, Monad, SemigroupK}
import derevo.derive
import mouse.any._
import org.ergoplatform.common.TraceId
import org.ergoplatform.common.streaming.syntax._
import org.ergoplatform.dex.domain.amm.CFMMOrder
import org.ergoplatform.dex.executor.amm.config.ExecutionConfig
import org.ergoplatform.dex.executor.amm.modules.CFMMBacklog
import org.ergoplatform.dex.executor.amm.services.Execution
import org.ergoplatform.dex.executor.amm.streaming.CFMMCircuit
import org.ergoplatform.ergo.services.explorer.TxSubmissionErrorParser
import tofu.Catches
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.streams.Evals
import tofu.syntax.context._
import tofu.syntax.embed._
import tofu.syntax.handle._
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.streams.all._
import tofu.syntax.time._

@derive(representableK)
trait Executor[F[_]] {

  def run: F[Unit]
}

object Executor {

  def make[
    I[_]: Functor,
    F[_]: Monad: SemigroupK: Defer: Evals[*[_], G]: ExecutionConfig.Has,
    G[_]: Monad: TraceId.Local: Clock: Catches
  ](implicit
    backlog: CFMMBacklog[G],
    service: Execution[G],
    logs: Logs[I, G]
  ): I[Executor[F]] =
    logs.forService[Executor[F]] map { implicit l =>
      ExecutionConfig.access
        .map(conf => new Live[F, G](conf): Executor[F])
        .embed
    }

  final private class Live[
    F[_]: Monad: SemigroupK: Defer: Evals[*[_], G],
    G[_]: Monad: Logging: TraceId.Local: Clock: Catches
  ](conf: ExecutionConfig)(implicit
    backlog: CFMMBacklog[G],
    service: Execution[G],
    errParser: TxSubmissionErrorParser
  ) extends Executor[F] {

    def run: F[Unit] =
      eval(backlog.pop).repeat
        .evalMap { order =>
          service
            .executeAttempt(order)
            .handleWith[Throwable](e => warnCause"Order execution failed fatally" (e) as none[CFMMOrder])
            .local(_ => TraceId.fromString(order.id.value))
            .tupleLeft(order)
        }
        .evalMap {
          case (_, None) => unit[G]
          case (_, Some(order)) =>
            now.millis >>= {
              case ts if ts - order.timestamp < conf.orderLifetime.toMillis =>
                warn"Failed to execute $order. Going to retry." >> backlog.put(order)
              case _ =>
                warn"Failed to execute $order. Order expired."
            }
        }
  }
}
