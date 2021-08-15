package org.ergoplatform.common.streaming

import cats.effect.Timer
import cats.{FlatMap, Monad}
import fs2.kafka.types.KafkaOffset
import tofu.higherKind.Embed
import tofu.streams.{Evals, ParFlatten}
import tofu.syntax.embed._
import tofu.syntax.monadic._
import tofu.syntax.streams.all._
import tofu.syntax.time.now

import scala.concurrent.duration.{FiniteDuration, MILLISECONDS}

trait StreamingCircuit[K, V, F[_], G[_]] {

  type Offset

  def stream: F[Committable[K, V, Offset, G]]

  def retry: F[(K, V)] => F[Unit]
}

object StreamingCircuit {

  type Aux[K, V, O, F[_], G[_]] = StreamingCircuit[K, V, F, G] { type Offset = O }

  def make[
    F[_]: Monad: Evals[*[_], G]: ParFlatten: RotationConfig.Has,
    G[_]: Monad: Timer,
    K,
    V
  ](implicit
    in: Consumer.Aux[K, V, KafkaOffset, F, G],
    retriesIn: Consumer.Aux[K, Delayed[V], KafkaOffset, F, G],
    retriesOut: Producer[K, Delayed[V], F]
  ): StreamingCircuit.Aux[K, V, KafkaOffset, F, G] =
    embed.embed(RotationConfig.access.map(conf => new Live[F, G, K, V](conf)))

  final private class StreamingCircuitContainer[K, V, O1, F[_]: FlatMap, G[_]](
    ffa: F[StreamingCircuit.Aux[K, V, O1, F, G]]
  ) extends StreamingCircuit[K, V, F, G] {
    type Offset = O1
    def stream: F[Committable[K, V, Offset, G]] = ffa >>= (_.stream)
    def retry: F[(K, V)] => F[Unit]             = fa => ffa >>= (_.retry(fa))
  }

  implicit def embed[K, V, O, G[_]]: Embed[Aux[K, V, O, *[_], G]] =
    new Embed[Aux[K, V, O, *[_], G]] {

      def embed[F[_]: FlatMap](ft: F[Aux[K, V, O, F, G]]): Aux[K, V, O, F, G] =
        new StreamingCircuitContainer(ft)
    }

  final class Live[
    F[_]: Monad: Evals[*[_], G]: ParFlatten,
    G[_]: Monad: Timer,
    K,
    V
  ](conf: RotationConfig)(implicit
    in: Consumer.Aux[K, V, KafkaOffset, F, G],
    retriesIn: Consumer.Aux[K, Delayed[V], KafkaOffset, F, G],
    retriesOut: Producer[K, Delayed[V], F]
  ) extends StreamingCircuit[K, V, F, G] {

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
