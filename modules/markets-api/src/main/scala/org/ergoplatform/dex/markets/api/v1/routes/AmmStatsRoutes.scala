package org.ergoplatform.dex.markets.api.v1.routes

import cats.effect.{Concurrent, ContextShift, Timer}
import cats.syntax.semigroupk._
import org.ergoplatform.common.http.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.common.http.HttpError
import org.ergoplatform.common.http.syntax._
import org.ergoplatform.dex.markets.api.v1.endpoints.AmmStatsEndpoints
import org.ergoplatform.dex.markets.api.v1.services.{AmmStats, LqLocks}
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}

final class AmmStatsRoutes[
  F[_]: Concurrent: ContextShift: Timer: AdaptThrowableEitherT[*[_], HttpError]
](stats: AmmStats[F], locks: LqLocks[F])(implicit opts: Http4sServerOptions[F, F]) {

  private val endpoints = new AmmStatsEndpoints()
  import endpoints._

  private val interpreter = Http4sServerInterpreter(opts)

  def routes: HttpRoutes[F] = getPoolLocksR <+> getPlatformStatsR <+> getPoolStatsR

  def getPoolLocksR: HttpRoutes[F] = interpreter.toRoutes(getPoolLocks) { case (poolId, leastDeadline) =>
    locks.byPool(poolId, leastDeadline).adaptThrowable.value
  }

  def getPoolStatsR: HttpRoutes[F] = interpreter.toRoutes(getPoolStats) { case (poolId, tw) =>
    stats.getPoolSummary(poolId, tw).adaptThrowable.orNotFound(s"PoolStats{poolId=$poolId}").value
  }

  def getPlatformStatsR: HttpRoutes[F] = interpreter.toRoutes(getPlatformStats) { tw =>
    stats.getPlatformSummary(tw).adaptThrowable.value
  }
}

object AmmStatsRoutes {

  def make[F[_]: Concurrent: ContextShift: Timer: AdaptThrowableEitherT[*[_], HttpError]](implicit
    stats: AmmStats[F],
    locks: LqLocks[F],
    opts: Http4sServerOptions[F, F]
  ): HttpRoutes[F] =
    new AmmStatsRoutes[F](stats, locks).routes
}
