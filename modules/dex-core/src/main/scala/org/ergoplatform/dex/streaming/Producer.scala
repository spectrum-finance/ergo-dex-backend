package org.ergoplatform.dex.streaming

import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import fs2._
import fs2.kafka.{ProducerRecord, ProducerRecords, ProducerSettings, produce => kafkaProduce}
import org.ergoplatform.dex.TopicId

trait Producer[F[_], A] {

  def produce: Pipe[F, A, Unit]
}

object Producer {

  final private class Live[F[_]: ConcurrentEffect: ContextShift: Timer, A: KeyEncoder](
    settings: ProducerSettings[F, String, A],
    topicId: TopicId
  ) extends Producer[F, A] {

    def produce: Pipe[F, A, Unit] =
      _.map { v =>
        ProducerRecords.one(ProducerRecord(topicId.value, KeyEncoder[A].encode(v), v))
      }.through(kafkaProduce[F, String, A, Unit](settings)).drain
  }
}
