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
    eval(print("00")) >>
    eval(print("One")).repeat
      .flatMap(_ => eval(print("Two")))

  program[fs2.Stream[IO, *], IO].compile.drain.unsafeRunSync()
}
