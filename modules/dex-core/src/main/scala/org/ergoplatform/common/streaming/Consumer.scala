package org.ergoplatform.common.streaming

import cats.tagless.FunctorK
import cats.{~>, Applicative, FlatMap, Functor, Monad, MonoidK}
import fs2.Stream
import fs2.kafka._
import fs2.kafka.types.KafkaOffset
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.ergoplatform.dex.configs.ConsumerConfig
import tofu.fs2.LiftStream
import tofu.higherKind.Embed
import tofu.streams.{Evals, ParFlatten}
import tofu.syntax.context._
import tofu.syntax.monadic._
import tofu.syntax.streams.all._

trait Consumer[K, V, F[_], G[_]] {

  type Offset

  def stream: F[Committable[K, V, Offset, G]]
}

object Consumer {

  type Aux[K, V, O, F[_], G[_]] = Consumer[K, V, F, G] { type Offset = O }

  final private class ConsumerContainer[K, V, O1, F[_]: FlatMap, G[_]](
    ffa: F[Consumer.Aux[K, V, O1, F, G]]
  ) extends Consumer[K, V, F, G] {

    type Offset = O1

    def stream: F[Committable[K, V, Offset, G]] =
      ffa.flatMap(_.stream)
  }

  implicit def embed[I[_], K, V, O]: Embed[Consumer.Aux[K, V, O, *[_], I]] =
    new Embed[Consumer.Aux[K, V, O, *[_], I]] {

      def embed[F[_]: FlatMap](ft: F[Consumer.Aux[K, V, O, F, I]]): Consumer.Aux[K, V, O, F, I] =
        new ConsumerContainer[K, V, O, F, I](ft)
    }

  implicit def functorK[I[_], K, V]: FunctorK[Consumer[K, V, *[_], I]] =
    new FunctorK[Consumer[K, V, *[_], I]] {

      def mapK[F[_], G[_]](af: Consumer[K, V, F, I])(fk: F ~> G): Consumer[K, V, G, I] =
        new Consumer[K, V, G, I] {
          type Offset = af.Offset
          def stream: G[Committable[K, V, Offset, I]] = fk(af.stream)
        }
    }

  def combine2[
    F[_]: Functor: ParFlatten: Evals[*[_], G],
    G[_]: Functor,
    K,
    V,
    VA,
    VB,
    O
  ](consumerA: Consumer.Aux[K, VA, O, F, G], consumerB: Consumer.Aux[K, VB, O, F, G])(
    fva: VA => V,
    fvb: VB => V
  ): Consumer.Aux[K, V, O, F, G] =
    new Consumer[K, V, F, G] {
      type Offset = O

      def stream: F[Committable[K, V, Offset, G]] =
        emits(
          List(
            consumerA.stream.map(c => Committable.functor.map(c)(fva)),
            consumerB.stream.map(c => Committable.functor.map(c)(fvb))
          )
        ).parFlatten(2)
    }

  def make[
    F[_]: Monad: LiftStream[*[_], G]: ConsumerConfig.Has,
    G[_]: Functor,
    K,
    V
  ](implicit makeConsumer: MakeKafkaConsumer[G, K, V]): Consumer.Aux[K, V, KafkaOffset, F, G] =
    embed.embed(context.map(make[F, G, K, V]))

  def make[
    F[_]: Monad: LiftStream[*[_], G],
    G[_]: Functor,
    K,
    V
  ](conf: ConsumerConfig)(implicit
    makeConsumer: MakeKafkaConsumer[G, K, V]
  ): Consumer.Aux[K, V, KafkaOffset, F, G] =
    functorK
      .mapK(new Live[K, V, G](conf))(LiftStream[F, G].liftF)
      .asInstanceOf[Consumer.Aux[K, V, Live[K, V, F]#Offset, F, G]]

  def empty[
    F[_],
    G[_],
    K,
    V
  ](implicit F: MonoidK[F]): Consumer.Aux[K, V, KafkaOffset, F, G] =
    new Consumer[K, V, F, G] {
      type Offset = (TopicPartition, OffsetAndMetadata)
      def stream: F[Committable[K, V, Offset, G]] = F.empty
    }

  final class Live[K, V, F[_]: Functor](config: ConsumerConfig)(implicit
    makeConsumer: MakeKafkaConsumer[F, K, V]
  ) extends Consumer[K, V, Stream[F, *], F] {

    type Offset = (TopicPartition, OffsetAndMetadata)

    def stream: Stream[F, Committable[K, V, Offset, F]] =
      makeConsumer(config)
        .evalTap(_.subscribeTo(config.topicId.value))
        .flatMap(_.stream)
        .map(KafkaCommittable(_))
  }
}
