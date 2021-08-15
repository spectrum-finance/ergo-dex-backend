package org.ergoplatform.common.http

import cats.Monad
import cats.data.{Kleisli, OptionT}
import org.ergoplatform.common.TraceId
import org.http4s.HttpRoutes
import org.typelevel.ci.CIString
import tofu.generate.GenUUID
import tofu.syntax.context._
import tofu.syntax.funk.funKFrom
import tofu.syntax.monadic._

/** Adds traceId to routes logic effect.
  * TraceId is obtained either from request header or from randomly generated UUID.
  */
object Tracing {

  val TraceIdHeader = "X-Trace-Id"

  def apply[F[_]: Monad: GenUUID: TraceId.Local](routes: HttpRoutes[F]): HttpRoutes[F] =
    Kleisli { req =>
      val traceIdF =
        req.headers
          .get(CIString(TraceIdHeader))
          .map(_.head.value.pure)
          .getOrElse(GenUUID[F].randomUUID.map(_.toString))
          .map(TraceId.fromString)
      for {
        traceId <- OptionT.liftF(traceIdF)
        trans = funKFrom[F](_.local(_ => traceId))
        tracedRoutes <- OptionT(trans(routes.run(req).value))
      } yield tracedRoutes
    }
}
