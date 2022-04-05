package org.ergoplatform.dex.markets.api.v1

import cats.effect.{Concurrent, ConcurrentEffect, ContextShift, Resource, Timer}
import cats.syntax.semigroupk._
import org.ergoplatform.common.TraceId
import org.ergoplatform.common.http.cache.CacheMiddleware.CachingMiddleware
import org.ergoplatform.common.http.config.HttpConfig
import org.ergoplatform.common.http.routes.unliftRoutes
import org.ergoplatform.dex.markets.api.v1.routes.{AmmStatsRoutes, DocsRoutes}
import org.ergoplatform.dex.markets.api.v1.services.{AmmStats, LqLocks}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import org.http4s.server.{Router, Server}
import org.http4s.syntax.kleisli._
import sttp.tapir.server.http4s.Http4sServerOptions
import tofu.lift.Unlift
import scala.concurrent.ExecutionContext

object HttpServer {

  def make[
    I[_]: ConcurrentEffect: ContextShift: Timer,
    F[_]: Concurrent: ContextShift: Timer: Unlift[*[_], I]: TraceId.Local
  ](conf: HttpConfig, ec: ExecutionContext)(implicit
    stats: AmmStats[F],
    locks: LqLocks[F],
    opts: Http4sServerOptions[F, F],
    cache: CachingMiddleware[F]
  ): Resource[I, Server] = {
    val ammStatsR = AmmStatsRoutes.make[F]
    val docsR     = DocsRoutes.make[F]
    val api       = Router("/" -> CORS(unliftRoutes[F, I](ammStatsR <+> docsR))).orNotFound
    BlazeServerBuilder[I](ec).bindHttp(conf.port, conf.host).withHttpApp(api).resource
  }
}
