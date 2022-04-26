package org.ergoplatform.common.http.cache

import cats.Monad
import cats.data.{Kleisli, OptionT}
import cats.effect.Sync
import org.ergoplatform.common.http.cache.types.Hash32
import org.http4s.{HttpRoutes, Status}
import tofu.syntax.embed._
import tofu.syntax.monadic._

object CacheMiddleware {

  def make[F[_]: Monad: Sync](cacheStatuses: List[Status])(implicit
    caching: HttpResponseCaching[F]
  ): CachingMiddleware[F] =
    new CachingMiddleware[F](caching, cacheStatuses)

  final class CachingMiddleware[F[_]: Monad: Sync](caching: HttpResponseCaching[F], cacheStatuses: List[Status]) {

    def middleware(routes: HttpRoutes[F]): HttpRoutes[F] = Kleisli { req =>
      OptionT(caching.process(req)).orElse {
        for {
          resp    <- routes(req)
          reqBody <- OptionT.liftF(req.body.compile.to(Seq))
          requestHash = Hash32(req.method.toString.getBytes, req.uri.toString.getBytes, reqBody)
          _ <- OptionT.liftF {
                 if (cacheStatuses.contains(resp.status)) caching.saveResponse(requestHash, resp)
                 else unit
               }
        } yield resp
      }
    }
  }
}
