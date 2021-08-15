package org.ergoplatform.dex

import cats.effect.{Clock, IO}

import scala.concurrent.duration.TimeUnit

trait CatsPlatform {

  val FixedTs = 1628107666776L

  implicit val clockIO: Clock[IO] =
    new Clock[IO] {
      def realTime(unit: TimeUnit): IO[Long] = IO.pure(FixedTs)
      def monotonic(unit: TimeUnit): IO[Long] = IO.pure(FixedTs)
    }
}
