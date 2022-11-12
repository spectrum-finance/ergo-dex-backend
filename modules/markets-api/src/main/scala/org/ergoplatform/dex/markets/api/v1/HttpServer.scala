package org.ergoplatform.dex.markets.api.v1

import cats.effect.{Concurrent, ConcurrentEffect, ContextShift, ExitCode, Timer}
import cats.syntax.semigroupk._
import org.ergoplatform.common.TraceId
import org.ergoplatform.common.http.cache.CacheMiddleware.CachingMiddleware
import org.ergoplatform.common.http.config.HttpConfig
import org.ergoplatform.common.http.routes.unliftRoutes
import org.ergoplatform.dex.markets.api.v1.routes.{AmmStatsRoutes, DocsRoutes}
import org.ergoplatform.dex.markets.api.v1.services.{AmmStats, LqLocks}
import org.ergoplatform.dex.markets.configs.RequestConfig
import org.ergoplatform.graphite.MetricsMiddleware.MetricsMiddleware
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import org.http4s.server.middleware.CORS
import sttp.tapir.server.http4s.Http4sServerOptions
import tofu.lift.Unlift

import scala.concurrent.ExecutionContext

object HttpServer {

  def make[
    I[_]: ConcurrentEffect: ContextShift: Timer,
    F[_]: Concurrent: ContextShift: Timer: Unlift[*[_], I]: TraceId.Local
  ](conf: HttpConfig, ec: ExecutionContext, requestConf: RequestConfig)(implicit
    stats: AmmStats[F],
    locks: LqLocks[F],
    opts: Http4sServerOptions[F, F],
    cache: CachingMiddleware[F],
    metrics: MetricsMiddleware[F]
  ): fs2.Stream[I, ExitCode] = {
    val ammStatsR  = AmmStatsRoutes.make[F](requestConf)
    val docsR      = DocsRoutes.make[F](requestConf)
    val routes     = unliftRoutes[F, I](metrics.middleware(cache.middleware(ammStatsR <+> docsR)))
    val corsRoutes = CORS.policy.withAllowOriginAll(routes)
    val api        = Router("/" -> corsRoutes).orNotFound
    BlazeServerBuilder[I](ec).bindHttp(conf.port, conf.host).withHttpApp(api).serve
  }
}
