package org.ergoplatform.dex.streaming

import cats.effect.{ConcurrentEffect, ContextShift}
import fs2._
import fs2.kafka.{ProducerRecords, _}
import org.ergoplatform.dex.configs.ProducerConfig

trait MakeKafkaProducer[F[_], K, V] {

  def apply(config: ProducerConfig): Pipe[F, ProducerRecords[K, V, Unit], Unit]
}

object MakeKafkaProducer {

  implicit def instance[
    F[_]: ConcurrentEffect: ContextShift,
    K: RecordSerializer[F, *],
    V: RecordSerializer[F, *]
  ]: MakeKafkaProducer[F, K, V] =
    (config: ProducerConfig) => {
      val producerSettings =
        ProducerSettings[F, K, V]
          .withBootstrapServers(config.bootstrapServers.mkString(","))
      _.through(produce(producerSettings)).drain
    }
}
