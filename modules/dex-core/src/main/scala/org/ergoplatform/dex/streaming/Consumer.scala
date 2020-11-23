package org.ergoplatform.dex.streaming

import cats.tagless.FunctorK
import cats.{~>, FlatMap, Functor, Monad}
import fs2.Stream
import fs2.kafka._
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.ergoplatform.dex.configs.ConsumerConfig
import tofu.WithContext
import tofu.fs2.LiftStream
import tofu.higherKind.Embed
import tofu.syntax.context._
import tofu.syntax.monadic._

import scala.annotation.unchecked.uncheckedVariance

trait Consumer[K, V, F[_], G[_]] {

  type O

  def stream: F[Committable[K, V, O, G]]
}

object Consumer {

  type Aux[K, V, O1, F[_], G[_]] = Consumer[K, V, F, G] { type O = O1 }

  final private class ConsumerContainer[K, V, F[_]: FlatMap, G[_]](
    ffa: F[Consumer[K, V, F, G]]
  ) extends Consumer[K, V, F, G] {

    type O = Any

    def stream: F[Committable[K, V, O, G]] =
      ffa.flatMap(_.stream.widen[Committable[K, V, O, G]])
  }

  implicit def embed[I[_], K, V]: Embed[Consumer[K, V, *[_], I]] =
    new Embed[Consumer[K, V, *[_], I]] {

      def embed[F[_]: FlatMap](ft: F[Consumer[K, V, F, I]]): Consumer[K, V, F, I] =
        new ConsumerContainer[K, V, F, I](ft)
    }

  implicit def functorK[I[_], K, V]: FunctorK[Consumer[K, V, *[_], I]] =
    new FunctorK[Consumer[K, V, *[_], I]] {

      def mapK[F[_], G[_]](af: Consumer[K, V, F, I])(fk: F ~> G): Consumer[K, V, G, I] =
        new Consumer[K, V, G, I] {
          type O = af.O
          def stream: G[Committable[K, V, O, I]] = fk(af.stream)
        }
    }

  def make[
    F[_]: Monad: LiftStream[*[_], G]: WithContext[*[_], ConsumerConfig],
    G[_]: Functor,
    K,
    V
  ](implicit makeConsumer: MakeKafkaConsumer[G, K, V]): Consumer[K, V, F, G] =
    embed.embed(
      context map (conf => functorK.mapK(new Live[K, V, G](conf))(LiftStream[F, G].liftF))
    )

  final class Live[K, V, F[_]: Functor](config: ConsumerConfig)(implicit
    makeConsumer: MakeKafkaConsumer[F, K, V]
  ) extends Consumer[K, V, Stream[F, *], F] {

    type O = (TopicPartition, OffsetAndMetadata)

    def stream: Stream[F, Committable[K, V, O, F]] =
      makeConsumer(config)
        .evalTap(_.subscribeTo(config.topicId.value))
        .flatMap(_.stream)
        .map(KafkaCommittable(_))
  }
}
