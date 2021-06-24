package org.ergoplatform.dex.resolver.http

import cats.effect.{Concurrent, ConcurrentEffect, ContextShift, ExitCode, Timer}
import org.ergoplatform.common.http.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.common.http.HttpError
import org.ergoplatform.common.http.routes.unliftRoutes
import org.ergoplatform.dex.resolver.config.HttpConfig
import org.ergoplatform.dex.resolver.services.Resolver
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import org.http4s.syntax.kleisli._
import sttp.tapir.server.http4s.Http4sServerOptions
import tofu.lift.Unlift

import scala.concurrent.ExecutionContext

object HttpServer {

  def make[
    I[_]: ConcurrentEffect: ContextShift: Timer,
    F[_]: Concurrent: ContextShift: Timer: Unlift[*[_], I]
  ](conf: HttpConfig, ec: ExecutionContext)(implicit
    resolver: Resolver[F],
    opts: Http4sServerOptions[F, F]
  ): fs2.Stream[I, ExitCode] = {
    val routes = new Routes(resolver).routes
    val api    = Router("/" -> unliftRoutes[F, I](routes)).orNotFound
    BlazeServerBuilder[I](ec).bindHttp(conf.port, conf.host).withHttpApp(api).serve
  }
}
