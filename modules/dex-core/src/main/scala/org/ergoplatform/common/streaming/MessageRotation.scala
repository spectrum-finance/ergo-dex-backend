package org.ergoplatform.common.streaming

import cats.Monad
import cats.effect.Timer
import fs2.kafka.types.KafkaOffset
import tofu.streams.Evals
import tofu.syntax.monadic._
import tofu.syntax.streams.all._
import tofu.syntax.time.now

import scala.concurrent.duration.{FiniteDuration, MILLISECONDS}

trait MessageRotation[K, V, F[_], G[_]] {

  type Offset

  def consume: F[Committable[K, V, Offset, G]]

  def retry: F[(K, V)] => F[Unit]
}

object MessageRotation {

  final class Live[
    F[_]: Monad: Evals[*[_], G],
    G[_]: Monad: Timer,
    K,
    V
  ](conf: RotationConfig)(implicit
    retriesIn: Consumer.Aux[K, Delayed[V], KafkaOffset, F, G],
    retriesOut: Producer[K, Delayed[V], F]
  ) extends MessageRotation[K, V, F, G] {

    type Offset = KafkaOffset

    def consume: F[Committable[K, V, Offset, G]] =
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
