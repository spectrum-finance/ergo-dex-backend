package org.ergoplatform.dex.streaming

import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import fs2.kafka.{consumerStream, AutoOffsetReset, ConsumerSettings, KafkaConsumer, RecordDeserializer}
import fs2.Stream
import org.ergoplatform.dex.configs.ConsumerConfig

/** Kafka consumer instance maker.
  */
trait MakeKafkaConsumer[K, V, F[_]] {

  def apply(config: ConsumerConfig): Stream[F, KafkaConsumer[F, K, V]]
}

object MakeKafkaConsumer {

  implicit def instance[
    F[_]: ConcurrentEffect: Timer: ContextShift,
    K: RecordDeserializer[F, *],
    V: RecordDeserializer[F, *]
  ]: MakeKafkaConsumer[K, V, F] = { (config: ConsumerConfig) =>
    val settings =
      ConsumerSettings[F, K, V]
        .withAutoOffsetReset(AutoOffsetReset.Earliest)
        .withBootstrapServers(config.bootstrapServers.toList.mkString(","))
        .withGroupId(config.groupId.value)
        .withClientId(config.clientId.value)
    consumerStream(settings)
  }
}
