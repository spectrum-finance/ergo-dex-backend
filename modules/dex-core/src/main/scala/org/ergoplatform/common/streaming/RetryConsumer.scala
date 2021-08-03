package org.ergoplatform.common.streaming

import cats.{FlatMap, Monad}
import cats.effect.Timer
import fs2.kafka.types.KafkaOffset
import tofu.streams.{Evals, ParFlatten}
import tofu.syntax.monadic._
import tofu.syntax.streams.all._
import tofu.syntax.time.now

import scala.concurrent.duration.{FiniteDuration, MILLISECONDS}

trait RetryConsumer[K, V, F[_], G[_]] {

  type Offset

  def stream: F[Committable[K, V, Offset, G]]

  def retry: F[(K, V)] => F[Unit]
}

object RetryConsumer {

  type Aux[K, V, O, F[_], G[_]] = RetryConsumer[K, V, F, G] { type Offset = O }

  private final class RetryConsumerContainer[K, V, O1, F[_]: FlatMap, G[_]]

  final class Live[
    F[_]: Monad: Evals[*[_], G]: ParFlatten,
    G[_]: Monad: Timer,
    K,
    V
  ](conf: RotationConfig)(implicit
    in: Consumer.Aux[K, V, KafkaOffset, F, G],
    retriesIn: Consumer.Aux[K, Delayed[V], KafkaOffset, F, G],
    retriesOut: Producer[K, Delayed[V], F]
  ) extends RetryConsumer[K, V, F, G] {

    type Offset = KafkaOffset

    def stream: F[Committable[K, V, Offset, G]] =
      emits(List(in.stream, processRetries)).parFlattenUnbounded

    def processRetries: F[Committable[K, V, Offset, G]] =
      retriesIn.stream.evalMap { c =>
        now.millis[G].flatMap { ts =>
          val sleep = c.message.blockedUntil - ts
          if (sleep > 0) Timer[G].sleep(FiniteDuration(sleep, MILLISECONDS))
          else unit[G]
        } as Committable.functor.map(c)(_.message)
      }

    def retry: F[(K, V)] => F[Unit] =
      fa =>
        retriesOut.produce(
          fa.evalMap { case (k, v) =>
            now.millis[G].map { ts =>
              val nextAttemptAt = ts + conf.retryDelay.toMillis
              Record(k, Delayed(v, nextAttemptAt))
            }
          }
        )
  }
}
