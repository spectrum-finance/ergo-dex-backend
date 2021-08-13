package org.ergoplatform.dex.executor.amm.processes

import cats.effect.Clock
import cats.syntax.option._
import cats.{Functor, Monad}
import derevo.derive
import mouse.any._
import org.ergoplatform.common.TraceId
import org.ergoplatform.common.streaming.syntax._
import org.ergoplatform.dex.domain.amm.CFMMOrder
import org.ergoplatform.dex.executor.amm.config.ExecutionConfig
import org.ergoplatform.dex.executor.amm.services.Execution
import org.ergoplatform.dex.executor.amm.streaming.CFMMCircuit
import org.ergoplatform.ergo.explorer.TxSubmissionErrorParser
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
    F[_]: Monad: Evals[*[_], G]: ExecutionConfig.Has,
    G[_]: Monad: TraceId.Local: Clock: Catches
  ](implicit
    orders: CFMMCircuit[F, G],
    service: Execution[G],
    logs: Logs[I, G]
  ): I[Executor[F]] =
    logs.forService[Executor[F]] map { implicit l =>
      ExecutionConfig.access
        .map(conf => new Live[F, G](conf): Executor[F])
        .embed
    }

  final private class Live[
    F[_]: Monad: Evals[*[_], G],
    G[_]: Monad: Logging: TraceId.Local: Clock: Catches
  ](conf: ExecutionConfig)(implicit
    orders: CFMMCircuit[F, G],
    service: Execution[G],
    errParser: TxSubmissionErrorParser
  ) extends Executor[F] {

    def run: F[Unit] =
      orders.stream
        .evalMap { rec =>
          service
            .executeAttempt(rec.message)
            .handleWith[Throwable](e => warnCause"Order execution failed fatally" (e) as none[CFMMOrder])
            .local(_ => TraceId.fromString(rec.message.id.value))
            .tupleLeft(rec)
        }
        .flatTap {
          case (_, None) => unit[F]
          case (_, Some(order)) =>
            eval(now.millis) >>= {
              case ts if ts - order.timestamp < conf.orderLifetime.toMillis =>
                eval(warn"Failed to execute $order. Going to retry.") >>
                  orders.retry((order.id -> order).pure[F])
              case _ =>
                eval(warn"Failed to execute $order. Order expired.")
            }
        }
        .evalMap { case (rec, _) => rec.commit }
  }
}
