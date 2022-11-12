package org.ergoplatform.graphite

import cats.Functor
import cats.effect.Sync
import cats.syntax.applicativeError._
import cats.syntax.functor._
import tofu.logging.Logs
import tofu.syntax.logging._

trait Metrics[F[_]] {
  def sendTs(key: String, value: Double): F[Unit]
  def sendCount(key: String, value: Double): F[Unit]
}

object Metrics {

  def create[I[_]: Functor, F[_]: Sync](implicit client: GraphiteClient[F], logs: Logs[I, F]): I[Metrics[F]] =
    logs.forService[Metrics[F]].map { implicit log =>
      new Metrics[F] {
        def sendTs(key: String, value: Double): F[Unit] =
          client
            .send(GraphitePoint.GraphitePointTs(key, value))
            .handleErrorWith(err => error"While sending ts metrics error ${err.getMessage} has occurred.")

        def sendCount(key: String, value: Double): F[Unit] =
          client
            .send(GraphitePoint.GraphitePointCount(key, value))
            .handleErrorWith(err => error"While sending count metrics error ${err.getMessage} has occurred.")
      }
    }
}
