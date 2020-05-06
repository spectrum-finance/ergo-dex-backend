package org.ergoplatform.dex.streaming

import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import cats.syntax.functor._
import fs2.Stream
import fs2.kafka._
import org.ergoplatform.dex.TopicId

import scala.concurrent.duration._

trait Consumer[F[_], A] {

  def consume(f: Option[A] => Stream[F, Unit]): Stream[F, Unit]
}

object Consumer {

  final private class Live[F[_]: ConcurrentEffect: ContextShift: Timer, A](
    settings: ConsumerSettings[F, String, Option[A]],
    topicId: TopicId
  ) extends Consumer[F, A] {

    def consume(f: Option[A] => Stream[F, Unit]): Stream[F, Unit] =
      consumerStream(settings)
        .evalTap(_.subscribeTo(topicId.value))
        .flatMap(_.stream)
        .evalMap { rec =>
          f(rec.record.value).compile.drain as rec.offset
        }
        .through(commitBatchWithin(500, 5.seconds))
  }
}
