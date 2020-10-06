package org.ergoplatform.dex.streaming

import cats.tagless.FunctorK
import cats.{Functor, ~>}
import fs2.Stream
import fs2.kafka._
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.ergoplatform.dex.TopicId
import tofu.fs2.LiftStream

trait Consumer[K, V, F[_], G[_]] {

  type O

  def stream: F[Committable[K, V, O, G]]
}

object Consumer {

  implicit def functorK[I[_], K, V]: FunctorK[Consumer[K, V, *[_], I]] =
    new FunctorK[Consumer[K, V, *[_], I]] {

      def mapK[F[_], G[_]](af: Consumer[K, V, F, I])(fk: F ~> G): Consumer[K, V, G, I] =
        new Consumer[K, V, G, I] {
          type O = af.O
          def stream: G[Committable[K, V, O, I]] = fk(af.stream)
        }
    }

  def make[F[_]: LiftStream[*[_], G], G[_]: Functor, K, V](settings: ConsumerSettings[G, K, V], topicId: TopicId)(
    implicit makeConsumer: MakeKafkaConsumer[K, V, G]
  ): Consumer[K, V, F, G] =
    functorK.mapK(new Live[K, V, G](settings, topicId))(LiftStream[F, G].liftF)

  final class Live[K, V, F[_]: Functor](settings: ConsumerSettings[F, K, V], topicId: TopicId)(implicit
    makeConsumer: MakeKafkaConsumer[K, V, F]
  ) extends Consumer[K, V, Stream[F, *], F] {

    type O = (TopicPartition, OffsetAndMetadata)

    def stream: Stream[F, Committable[K, V, O, F]] =
      makeConsumer(settings)
        .evalTap(_.subscribeTo(topicId.value))
        .flatMap(_.stream)
        .map(KafkaCommittable(_))
  }
}
