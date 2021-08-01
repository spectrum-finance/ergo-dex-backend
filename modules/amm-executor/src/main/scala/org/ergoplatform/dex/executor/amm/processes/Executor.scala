package org.ergoplatform.dex.executor.amm.processes

import cats.effect.Clock
import cats.{Functor, Monad}
import derevo.derive
import mouse.any._
import org.ergoplatform.common.TraceId
import org.ergoplatform.common.streaming.syntax._
import org.ergoplatform.common.streaming.{Delayed, Record, RotationConfig}
import org.ergoplatform.dex.executor.amm.domain.errors.ExecutionFailed
import org.ergoplatform.dex.executor.amm.services.Execution
import org.ergoplatform.dex.executor.amm.streaming.{CFMMConsumer, OrdersRotation}
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.streams.Evals
import tofu.syntax.context._
import tofu.syntax.embed._
import tofu.syntax.monadic._
import tofu.syntax.streams.all._
import tofu.syntax.time._
import tofu.syntax.logging._

@derive(representableK)
trait Executor[F[_]] {

  def run: F[Unit]
}

object Executor {

  def make[
    I[_]: Functor,
    F[_]: Monad: Evals[*[_], G]: RotationConfig.Has: ExecutionFailed.Handle,
    G[_]: Monad: TraceId.Local: Clock
  ](implicit
    orders: CFMMConsumer[F, G],
    ordersRotation: OrdersRotation[F],
    service: Execution[G],
    logs: Logs[I, G]
  ): I[Executor[F]] =
    logs.forService[Executor[F]] map { implicit l =>
      RotationConfig.access
        .map(rotationConf => new Live[F, G](rotationConf): Executor[F])
        .embed
    }

  final private class Live[
    F[_]: Monad: Evals[*[_], G]: ExecutionFailed.Handle,
    G[_]: Monad: Logging: TraceId.Local: Clock
  ](rotationConf: RotationConfig)(implicit
    orders: CFMMConsumer[F, G],
    ordersRotation: OrdersRotation[F],
    service: Execution[G]
  ) extends Executor[F] {

    def run: F[Unit] =
      orders.stream
        .evalMap { rec =>
          service
            .executeAttempt(rec.message)
            .local(_ => TraceId.fromString(rec.message.id.value))
            .tupleLeft(rec)
        }
        .thrush { fa =>
          fa.flatTap {
            case (_, None) => unit[F]
            case (_, Some(order)) =>
              val toRotate =
                fa.evalMap { _ =>
                  now.millis.map { ts =>
                    val nextAttemptAt = ts + rotationConf.retryDelay.toMillis
                    Record(order.id, Delayed(order, nextAttemptAt))
                  }
                }
              eval(warn"Failed to execute $order, going to retry in ${rotationConf.retryDelay}") >>
              ordersRotation.produce(toRotate)
          }
        }
        .evalMap { case (rec, _) => rec.commit }
  }
}
