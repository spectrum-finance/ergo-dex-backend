package org.ergoplatform.dex.demo

import cats.effect.{IO, Sync}
import cats.{Defer, Monad, MonoidK}
import tofu.streams.{Broadcast, Evals, Pace}
import tofu.syntax.monadic._
import tofu.syntax.streams.all._
import tofu.fs2Instances._

object Streams extends App {

  def print[F[_]: Sync](s: String) = Sync[F].delay(println(s))

  def program[
    F[_]: Monad: Evals[*[_], G]: Defer: MonoidK,
    G[_]: Sync
  ] =
    emits(List(emits(List.empty[Unit]), eval(print("Finalize")))).flatten

  program[fs2.Stream[IO, *], IO].compile.drain.unsafeRunSync()
}
