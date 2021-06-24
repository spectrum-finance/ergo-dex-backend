package org.ergoplatform.dex.resolver.http

import cats.effect.{Concurrent, ContextShift, Timer}
import cats.syntax.semigroupk._
import org.ergoplatform.common.http.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.common.http.HttpError
import org.ergoplatform.common.http.syntax._
import org.ergoplatform.dex.resolver.services.Resolver
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}

final class Routes[
  F[_]: Concurrent: ContextShift: Timer: AdaptThrowableEitherT[*[_], HttpError]
](service: Resolver[F])(implicit opts: Http4sServerOptions[F, F]) {

  val routes: HttpRoutes[F] = resolveR <+> putPredictedR

  def resolveR: HttpRoutes[F] =
    Http4sServerInterpreter.toRoutes(Endpoints.resolve) { id =>
      service
        .resolve(id)
        .adaptThrowable
        .orNotFound(s"Route{id='$id'}")
        .value
    }

  def putPredictedR: HttpRoutes[F] =
    Http4sServerInterpreter.toRoutes(Endpoints.putPredicted) { pool =>
      service.put(pool).adaptThrowable.value
    }
}
