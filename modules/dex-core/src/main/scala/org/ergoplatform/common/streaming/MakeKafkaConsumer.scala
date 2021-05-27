package org.ergoplatform.common.streaming

import cats.effect.{Concurrent, ConcurrentEffect, ContextShift, Timer}
import cats.{FlatMap, Functor}
import fs2.Stream
import fs2.kafka._
import org.ergoplatform.dex.configs.{ConsumerConfig, KafkaConfig}
import tofu.higherKind.Embed
import tofu.lift.Unlift
import tofu.syntax.context._
import tofu.syntax.embed._
import tofu.syntax.monadic._
import tofu.syntax.unlift._

/** Kafka consumer instance maker.
  */
trait MakeKafkaConsumer[F[_], K, V] {

  def apply(config: ConsumerConfig): Stream[F, KafkaConsumer[F, K, V]]
}

object MakeKafkaConsumer {

  final private class MakeKafkaConsumerContainer[F[_]: Functor, K, V](ft: F[MakeKafkaConsumer[F, K, V]])
    extends MakeKafkaConsumer[F, K, V] {

    def apply(config: ConsumerConfig): Stream[F, KafkaConsumer[F, K, V]] =
      Stream.force(ft.map(_(config)))
  }

  implicit def embed[K, V]: Embed[MakeKafkaConsumer[*[_], K, V]] =
    new Embed[MakeKafkaConsumer[*[_], K, V]] {

      def embed[F[_]: FlatMap](ft: F[MakeKafkaConsumer[F, K, V]]): MakeKafkaConsumer[F, K, V] =
        new MakeKafkaConsumerContainer(ft)
    }

  def make[
    I[_]: ConcurrentEffect,
    F[_]: Concurrent: Timer: ContextShift: KafkaConfig.Has,
    K: RecordDeserializer[F, *],
    V: RecordDeserializer[F, *]
  ](implicit U: Unlift[I, F]): MakeKafkaConsumer[F, K, V] =
    embed.embed(
      context >>= { kafka =>
        U.concurrentEffect.map { implicit ce =>
          new MakeKafkaConsumer[F, K, V] {
            def apply(config: ConsumerConfig): Stream[F, KafkaConsumer[F, K, V]] = {
              val settings =
                ConsumerSettings[F, K, V]
                  .withAutoOffsetReset(AutoOffsetReset.Earliest)
                  .withBootstrapServers(kafka.bootstrapServers.mkString(","))
                  .withGroupId(config.groupId.value)
                  .withClientId(config.clientId.value)
              KafkaConsumer.stream(settings)
            }
          }
        }
      }
    )
}
