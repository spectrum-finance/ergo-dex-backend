package org.ergoplatform.dex.markets.api.v1.routes

import cats.effect.{Concurrent, ContextShift, Timer}
import org.ergoplatform.common.http.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.common.http.HttpError
import org.ergoplatform.common.http.syntax._
import org.ergoplatform.dex.markets.api.v1.endpoints.AmmStatsEndpoints
import org.ergoplatform.dex.markets.api.v1.services.AmmStats
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}

final class AmmStatsRoutes[
  F[_]: Concurrent: ContextShift: Timer: AdaptThrowableEitherT[*[_], HttpError]
](service: AmmStats[F])(implicit opts: Http4sServerOptions[F, F]) {

  private val endpoints = new AmmStatsEndpoints()
  import endpoints._

  private val interpreter = Http4sServerInterpreter(opts)

  def routes: HttpRoutes[F] = getPoolStatsR

  def getPoolStatsR: HttpRoutes[F] = interpreter.toRoutes(getPoolStats) { case (poolId, tw) =>
    service.getPoolSummary(poolId, tw).adaptThrowable.orNotFound(s"PoolStats{poolId=$poolId}").value
  }

  def getPlatformStatsR: HttpRoutes[F] = interpreter.toRoutes(getPlatformStats) { tw =>
    service.getPlatformSummary(tw).adaptThrowable.value
  }
}

object AmmStatsRoutes {

  def make[F[_]: Concurrent: ContextShift: Timer: AdaptThrowableEitherT[*[_], HttpError]](implicit
    service: AmmStats[F],
    opts: Http4sServerOptions[F, F]
  ): HttpRoutes[F] =
    new AmmStatsRoutes[F](service).routes
}
