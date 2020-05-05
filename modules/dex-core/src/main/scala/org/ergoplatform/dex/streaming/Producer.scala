package org.ergoplatform.dex.streaming

import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import fs2._
import fs2.kafka.{ProducerRecord, ProducerRecords, ProducerSettings, produce => kafkaProduce}

trait Producer[F[_], A] {

  def produce: Pipe[F, (String, A), Unit]
}

object Producer {

  final private class Live[F[_]: ConcurrentEffect: ContextShift: Timer, A](
    settings: ProducerSettings[F, String, A]
  ) extends Producer[F, A] {

    def produce: Pipe[F, (String, A), Unit] =
      _.map { case (k, v) =>
        ProducerRecords.one(ProducerRecord("topic", k, v))  // todo:
      }.through(kafkaProduce[F, String, A, Unit](settings)).drain
  }
}
