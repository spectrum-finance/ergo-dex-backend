package org.ergoplatform.graphite

import cats.Monad
import cats.data.{Kleisli, OptionT}
import cats.effect.Clock
import org.http4s.HttpRoutes

import java.util.concurrent.TimeUnit

object MetricsMiddleware {

  def make[F[_]: Monad: Clock](implicit
    metrics: Metrics[F]
  ): MetricsMiddleware[F] =
    new MetricsMiddleware[F](metrics)

  final class MetricsMiddleware[F[_]: Monad: Clock](metrics: Metrics[F]) {

    def middleware(routes: HttpRoutes[F]): HttpRoutes[F] = Kleisli { req =>
      val key = req.pathInfo.renderString.replaceAll("/", ".").drop(1)
      for {
        start  <- OptionT.liftF(Clock[F].realTime(TimeUnit.MILLISECONDS))
        resp   <- routes(req)
        finish <- OptionT.liftF(Clock[F].realTime(TimeUnit.MILLISECONDS))
        _      <- OptionT.liftF(metrics.send(key, (finish - start).toDouble))
      } yield resp
    }
  }
}
