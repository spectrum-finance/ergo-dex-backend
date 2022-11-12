package org.ergoplatform.graphite

import cats.Functor

import java.util.concurrent.TimeUnit
import cats.effect.{Clock, Sync}
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import tofu.logging.Logs
import tofu.syntax.logging._

trait Metrics[F[_]] {
  def send(key: String, value: Double): F[Unit]
}

object Metrics {

  def create[I[_]: Functor, F[_]: Clock: Sync](implicit client: GraphiteClient[F], logs: Logs[I, F]): I[Metrics[F]] =
    logs.forService[Metrics[F]].map { implicit log => (key: String, value: Double) =>
      Clock[F].realTime(TimeUnit.SECONDS).flatMap { time =>
        client
          .send(GraphitePoint(key, value, time))
          .flatTap(_ => info"Metrics were sent: $key -> $value -> $time.")
          .handleErrorWith(err => error"While sending metrics error ${err.getMessage} has occurred.")
      }
    }
}
