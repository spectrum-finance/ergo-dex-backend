package org.ergoplatform.common.http.cache

import cats.Monad
import cats.data.{Kleisli, OptionT}
import org.http4s.HttpRoutes
import tofu.syntax.embed._

object CacheMiddleware {

  def make[F[_]: Monad](implicit
    caching: HttpResponseCaching[F]
  ): CachingMiddleware[F] =
    new CachingMiddleware[F](caching)

  final class CachingMiddleware[F[_]: Monad](caching: HttpResponseCaching[F]) {

    def middleware(routes: HttpRoutes[F]): HttpRoutes[F] = Kleisli { req =>
      OptionT(caching.process(req)).orElse {
        for {
          resp <- routes(req)
          _    <- OptionT.liftF(caching.saveResponse(req.hashCode, resp))
        } yield resp
      }
    }
  }
}
