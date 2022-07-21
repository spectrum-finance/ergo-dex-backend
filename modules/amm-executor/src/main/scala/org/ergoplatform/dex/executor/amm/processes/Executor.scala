package org.ergoplatform.dex.executor.amm.processes

import cats.effect.{Clock, Timer}
import cats.syntax.option._
import cats.syntax.traverse._
import cats.{Defer, Functor, Monad, SemigroupK}
import derevo.derive
import mouse.any._
import org.ergoplatform.common.TraceId
import org.ergoplatform.common.streaming.syntax._
import org.ergoplatform.dex.domain.amm.CFMMOrder
import org.ergoplatform.dex.executor.amm.config.ExecutionConfig
import org.ergoplatform.dex.executor.amm.services.{CFMMBacklog, Execution}
import org.ergoplatform.dex.executor.amm.streaming.{CFMMCircuit, CFMMHistConsumer}
import org.ergoplatform.ergo.services.explorer.TxSubmissionErrorParser
import tofu.Catches
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.streams.{Evals, ParFlatten}
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
    F[_]: Monad: Evals[*[_], G]: ExecutionConfig.Has: Defer: SemigroupK: ParFlatten,
    G[_]: Monad: TraceId.Local: Clock: Catches: Timer
  ](implicit
    orders: CFMMCircuit[F, G],
    executedOrders: CFMMHistConsumer[F, G],
    cfmmBacklog: CFMMBacklog[G],
    service: Execution[G],
    logs: Logs[I, G]
  ): I[Executor[F]] =
    logs.forService[Executor[F]] map { implicit l =>
      ExecutionConfig.access
        .map(conf => new Live[F, G](conf): Executor[F])
        .embed
    }

  final private class Live[
    F[_]: Monad: Evals[*[_], G]: Defer: SemigroupK: ParFlatten,
    G[_]: Monad: Logging: Catches: Timer
  ](conf: ExecutionConfig)(implicit
    orders: CFMMCircuit[F, G],
    executedOrders: CFMMHistConsumer[F, G],
    backlog: CFMMBacklog[G],
    service: Execution[G],
    errParser: TxSubmissionErrorParser
  ) extends Executor[F] {

    def run: F[Unit] =
      emits(
        List(
          addToBacklog,
          executeOrders,
          dropExecuted
        )
      ).parFlattenUnbounded

    def addToBacklog: F[Unit] =
      orders.stream
        .evalTap(orderRec =>
          backlog.put(orderRec.message)
            .handleWith[Throwable](e => warnCause"Attempt to add order to backlog failed." (e))
        )
        .evalMap(_.commit)

    def dropExecuted: F[Unit] =
      executedOrders.stream
        .evalTap { rec =>
          rec.message.traverse(order =>
            backlog.drop(order.order.id)
              .handleWith[Throwable](e => warnCause"Attempt to drop order from backlog failed." (e))
          )
        }
        .evalMap(_.commit)

    def executeOrders: F[Unit] =
      eval(backlog.get).evalMap {
        case Some(order) => executeOrder(order)
        case None        => trace"No orders to execute. Going to wait for" >> Timer[G].sleep(conf.order)
      }.repeat

    private def executeOrder(order: CFMMOrder): G[Unit] =
      service
        .executeAttempt(order)
        .handleWith[Throwable](e => warnCause"Order execution failed fatally" (e) as none[CFMMOrder])
        .flatMap {
          case Some(order) => backlog.suspend(order)
          case None        => backlog.checkLater(order)
        }
  }
}
