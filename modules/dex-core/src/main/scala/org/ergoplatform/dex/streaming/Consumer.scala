package org.ergoplatform.dex.streaming

import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import cats.syntax.functor._
import cats.syntax.flatMap._
import fs2.{Chunk, Stream}
import fs2.kafka._
import org.ergoplatform.dex.TopicId

import scala.concurrent.duration._

trait Consumer[F[_], A] {

  /** Consume single element from upstream.
    */
  def consume(f: Option[A] => Stream[F, Unit]): Stream[F, Unit]

  /** Consume batch of elements received within a time window
    * or limited by the number of the elements, whichever happens first.
    */
  def consumeBatch(n: Int, duration: FiniteDuration)(
    f: Chunk[Option[A]] => Stream[F, Unit]
  ): Stream[F, Unit]
}

object Consumer {

  final private class Live[F[_]: ConcurrentEffect: ContextShift: Timer, A](
    settings: ConsumerSettings[F, String, Option[A]],
    topicId: TopicId
  ) extends Consumer[F, A] {

    def consume(f: Option[A] => Stream[F, Unit]): Stream[F, Unit] =
      stream
        .evalMap { rec =>
          f(rec.record.value).compile.drain as rec.offset
        }
        .through(commitBatchWithin(500, 5.seconds))

    def consumeBatch(n: Int, duration: FiniteDuration)(
      f: Chunk[Option[A]] => Stream[F, Unit]
    ): Stream[F, Unit] =
      stream.groupWithin(n, duration) >>= { xs =>
        f(xs.map(_.record.value)) >> Stream.eval(
          CommittableOffsetBatch.fromFoldable(xs.map(_.offset)).commit
        )
      }

    private def stream =
      consumerStream(settings)
        .evalTap(_.subscribeTo(topicId.value))
        .flatMap(_.stream)
  }
}
