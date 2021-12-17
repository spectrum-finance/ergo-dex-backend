package org.ergoplatform.dex.markets.api.v1.routes

import cats.effect.{Concurrent, ContextShift, Timer}
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.option._
import cats.syntax.semigroupk._
import org.ergoplatform.common.http.HttpError
import org.ergoplatform.dex.markets.api.v1.endpoints.{AmmStatsEndpoints, DocsEndpoints}
import org.http4s.HttpRoutes
import sttp.tapir.apispec.Tag
import sttp.tapir.docs.openapi._
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.redoc.http4s.RedocHttp4s
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}

final class DocsRoutes[F[_]: Concurrent: ContextShift: Timer](implicit
  opts: Http4sServerOptions[F, F]
) {

  private val endpoints = DocsEndpoints
  import endpoints._

  private val interpreter = Http4sServerInterpreter(opts)

  val routes: HttpRoutes[F] = openApiSpecR <+> redocApiSpecR

  val statsEndpoints = new AmmStatsEndpoints

  private def allEndpoints =
    statsEndpoints.endpoints

  private def tags =
    Tag(statsEndpoints.PathPrefix, "AMM Statistics".some) ::
    Nil

  private val docsAsYaml =
    OpenAPIDocsInterpreter()
      .toOpenAPI(allEndpoints, "Cardano Explorer API v1", "1.0")
      .tags(tags)
      .toYaml

  private def openApiSpecR: HttpRoutes[F] =
    interpreter.toRoutes(apiSpecDef) { _ =>
      docsAsYaml
        .asRight[HttpError]
        .pure[F]
    }

  private def redocApiSpecR: HttpRoutes[F] =
    new RedocHttp4s(
      "Redoc",
      docsAsYaml,
      "openapi",
      contextPath = "docs" :: Nil
    ).routes
}

object DocsRoutes {

  def make[F[_]: Concurrent: ContextShift: Timer](implicit opts: Http4sServerOptions[F, F]): HttpRoutes[F] =
    new DocsRoutes[F]().routes
}
