package org.ergoplatform.dex.streaming

import cats.effect.{Blocker, Concurrent, ConcurrentEffect, ContextShift, Timer}
import cats.syntax.functor._
import fs2.kafka._
import fs2.{kafka, Stream}
import org.ergoplatform.dex.settings.KafkaSettings

import scala.concurrent.duration._

abstract class Consumer[F[_], S[_[_] <: F[_], _], A] {

  /** Stream of outputs appearing in the blockchain.
    */
  def processStream(maxConcurrent: Int)(processor: Option[A] => F[Unit]): Stream[F, Unit]
}

object Consumer {

  def apply[F[_]: ConcurrentEffect: ContextShift: Timer, A](
    settings: KafkaSettings
  )(
    implicit
    blocker: Blocker,
    deserializer: RecordDeserializer[F, Option[A]]
  ): Consumer[F, Stream, A] = {
    val consumerStream = kafka.consumerStream[F]
    val consumerSettings =
      ConsumerSettings[F, String, Option[A]]
        .withBlocker(blocker)
        .withAutoOffsetReset(AutoOffsetReset.Earliest)
        .withGroupId(settings.groupId)
        .withClientId(settings.clientId)
    new Live(consumerStream, consumerSettings, settings.topicId)
  }

  final private class Live[F[_]: ContextShift: Timer: Concurrent, A](
    consumerStream: ConsumerStream[F],
    settings: ConsumerSettings[F, String, Option[A]],
    topicId: String
  ) extends Consumer[F, Stream, A] {

    def processStream(maxConcurrent: Int)(processor: Option[A] => F[Unit]): Stream[F, Unit] =
      consumerStream
        .using(settings)
        .evalTap(_.subscribeTo(topicId))
        .flatMap(_.stream)
        .mapAsync(maxConcurrent) { rec =>
          processor(rec.record.value) as rec.offset
        }
        .through(commitBatchWithin(500, 15.seconds))
  }
}
