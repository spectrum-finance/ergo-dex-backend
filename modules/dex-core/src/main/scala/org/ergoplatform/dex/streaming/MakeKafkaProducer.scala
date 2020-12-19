package org.ergoplatform.dex.streaming

import cats.effect.{Concurrent, ContextShift}
import fs2._
import fs2.kafka.{ProducerRecords, _}
import org.ergoplatform.dex.configs.ProducerConfig

trait MakeKafkaProducer[F[_], K, V] {

  def apply(config: ProducerConfig): Pipe[F, ProducerRecords[K, V, Unit], Unit]
}

object MakeKafkaProducer {

  def make[
    F[_]: Concurrent: ContextShift,
    K: RecordSerializer[F, *],
    V: RecordSerializer[F, *]
  ]: MakeKafkaProducer[F, K, V] = { (config: ProducerConfig) =>
    val producerSettings =
      ProducerSettings[F, K, V]
        .withBootstrapServers(config.bootstrapServers.mkString(","))
    _.through(KafkaProducer.pipe(producerSettings)).drain
  }
}
