package org.ergoplatform.dex.streaming

import cats.tagless.FunctorK
import cats.{Functor, ~>}
import fs2.Stream
import fs2.kafka._
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.ergoplatform.dex.TopicId
import tofu.fs2.LiftStream

trait Consumer[A, F[_], G[_]] {

  type O

  def stream: F[Committable[A, O, G]]
}

object Consumer {

  implicit def functorK[I[_], A]: FunctorK[Consumer[A, *[_], I]] =
    new FunctorK[Consumer[A, *[_], I]] {

      def mapK[F[_], G[_]](af: Consumer[A, F, I])(fk: F ~> G): Consumer[A, G, I] =
        new Consumer[A, G, I] {
          type O = af.O
          def stream: G[Committable[A, O, I]] = fk(af.stream)
        }
    }

  def make[F[_]: LiftStream[*[_], G], G[_]: Functor, A](settings: ConsumerSettings[G, String, A], topicId: TopicId)(
    implicit makeConsumer: MakeKafkaConsumer[A, G]
  ): Consumer[A, F, G] =
    functorK.mapK(new Live[A, G](settings, topicId))(LiftStream[F, G].liftF)

  final class Live[A, F[_]: Functor](settings: ConsumerSettings[F, String, A], topicId: TopicId)(implicit
    makeConsumer: MakeKafkaConsumer[A, F]
  ) extends Consumer[A, Stream[F, *], F] {

    type O = (TopicPartition, OffsetAndMetadata)

    def stream: Stream[F, Committable[A, O, F]] =
      makeConsumer(settings)
        .evalTap(_.subscribeTo(topicId.value))
        .flatMap(_.stream)
        .map(KafkaCommittable(_))
  }
}
