package org.ergoplatform.dex.resolver.http

import cats.effect.{Concurrent, ContextShift, Timer}
import cats.syntax.semigroupk._
import org.ergoplatform.common.TraceId
import org.ergoplatform.common.http.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.common.http.{HttpError, Tracing}
import org.ergoplatform.common.http.syntax._
import org.ergoplatform.dex.resolver.services.Resolver
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}

final class Routes[
  F[_]: Concurrent: ContextShift: Timer: AdaptThrowableEitherT[*[_], HttpError]: TraceId.Local
](service: Resolver[F])(implicit opts: Http4sServerOptions[F, F]) {

  val routes: HttpRoutes[F] = Tracing(resolveR <+> putPredictedR <+> invalidateR)

  private def interpreter = Http4sServerInterpreter(opts)

  def resolveR: HttpRoutes[F] =
    interpreter.toRoutes(Endpoints.resolve) { id =>
      service
        .resolve(id)
        .adaptThrowable
        .orNotFound(s"Route{id='$id'}")
        .value
    }

  def putPredictedR: HttpRoutes[F] =
    interpreter.toRoutes(Endpoints.putPredicted) { pool =>
      service.put(pool).adaptThrowable.value
    }

  def invalidateR: HttpRoutes[F] =
    interpreter.toRoutes(Endpoints.invalidate) { boxId =>
      service.invalidate(boxId).adaptThrowable.value
    }
}
