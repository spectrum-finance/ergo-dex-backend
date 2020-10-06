package org.ergoplatform.dex.streaming

import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import fs2.kafka.{consumerStream, ConsumerSettings, KafkaConsumer}
import fs2.Stream

/** Kafka consumer instance maker.
  */
trait MakeKafkaConsumer[K, V, F[_]] {

  def apply(settings: ConsumerSettings[F, K, V]): Stream[F, KafkaConsumer[F, K, V]]
}

object MakeKafkaConsumer {

  implicit def instance[F[_]: ConcurrentEffect: Timer: ContextShift, K, V]: MakeKafkaConsumer[K, V, F] =
    (settings: ConsumerSettings[F, K, V]) => consumerStream(settings)
}
