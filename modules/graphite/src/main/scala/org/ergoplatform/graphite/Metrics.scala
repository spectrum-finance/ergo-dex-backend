package org.ergoplatform.graphite

import cats.Functor
import cats.effect.{Clock, Sync}
import cats.syntax.applicativeError._
import org.ergoplatform.graphite.GraphitePoint.GraphitePointUdp
import tofu.logging.Logs
import tofu.syntax.logging._
import tofu.syntax.monadic._

import java.util.concurrent.TimeUnit

trait Metrics[F[_]] {
  def send(key: String, value: Double): F[Unit]
}

object Metrics {

  def create[I[_]: Functor, F[_]: Sync: Clock](implicit client: GraphiteClient[F], logs: Logs[I, F]): I[Metrics[F]] =
    logs.forService[Metrics[F]].map { implicit log =>
      (key: String, value: Double) => Clock[F]
        .realTime(TimeUnit.SECONDS)
        .flatMap { ts =>
          client
            .send(GraphitePointUdp(key, value, ts))
            .handleErrorWith(err => error"While sending count metrics error ${err.getMessage} has occurred.")
        }
    }
}
