package org.ergoplatform.dex

import cats.effect.{Clock, ExitCode, IO, IOApp}

import scala.concurrent.duration.TimeUnit

trait CatsPlatform extends IOApp {

  def run(args: List[String]): IO[ExitCode] = IO.pure(ExitCode.Success)

  val FixedTs = 1628107666776L

  implicit val clockIO: Clock[IO] =
    new Clock[IO] {
      def realTime(unit: TimeUnit): IO[Long] = IO.pure(FixedTs)
      def monotonic(unit: TimeUnit): IO[Long] = IO.pure(FixedTs)
    }
}
