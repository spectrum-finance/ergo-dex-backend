package org.ergoplatform.dex.streaming

import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import fs2.kafka.{consumerStream, ConsumerSettings, KafkaConsumer}
import fs2.Stream

/** Kafka consumer instance maker.
  */
trait MakeKafkaConsumer[A, F[_]] {

  def apply(settings: ConsumerSettings[F, String, A]): Stream[F, KafkaConsumer[F, String, A]]
}

object MakeKafkaConsumer {

  implicit def instance[F[_]: ConcurrentEffect: Timer: ContextShift, A]: MakeKafkaConsumer[A, F] =
    (settings: ConsumerSettings[F, String, A]) => consumerStream(settings)
}
