package org.ergoplatform.dex.streaming

import cats.{FlatMap, Functor}
import cats.effect.{ConcurrentEffect, ContextShift}
import fs2._
import fs2.kafka.{ProducerRecords, _}
import org.ergoplatform.dex.configs.ProducerConfig
import tofu.higherKind.Embed
import tofu.lift.Unlift
import tofu.syntax.monadic._
import tofu.syntax.unlift._

trait MakeKafkaProducer[F[_], K, V] {

  def apply(config: ProducerConfig): Pipe[F, ProducerRecords[K, V, Unit], Unit]
}

object MakeKafkaProducer {

  final private class MakeKafkaProducerContainer[F[_]: Functor, K, V](ft: F[MakeKafkaProducer[F, K, V]])
    extends MakeKafkaProducer[F, K, V] {

    def apply(config: ProducerConfig): Pipe[F, ProducerRecords[K, V, Unit], Unit] =
      s => Stream.force(ft.map(_.apply(config)(s)))
  }

  implicit def embed[K, V]: Embed[MakeKafkaProducer[*[_], K, V]] =
    new Embed[MakeKafkaProducer[*[_], K, V]] {

      def embed[F[_]: FlatMap](ft: F[MakeKafkaProducer[F, K, V]]): MakeKafkaProducer[F, K, V] =
        new MakeKafkaProducerContainer(ft)
    }

  def make[
    I[_]: ConcurrentEffect,
    F[_]: FlatMap: ContextShift,
    K: RecordSerializer[F, *],
    V: RecordSerializer[F, *]
  ](implicit U: Unlift[I, F]): MakeKafkaProducer[F, K, V] =
    embed.embed(
      U.concurrentEffect.map { implicit ce =>
        new MakeKafkaProducer[F, K, V] {
          def apply(config: ProducerConfig): Pipe[F, ProducerRecords[K, V, Unit], Unit] = {
            val producerSettings =
              ProducerSettings[F, K, V]
                .withBootstrapServers(config.bootstrapServers.mkString(","))
            _.through(produce(producerSettings)).drain
          }
        }
      }
    )
}
