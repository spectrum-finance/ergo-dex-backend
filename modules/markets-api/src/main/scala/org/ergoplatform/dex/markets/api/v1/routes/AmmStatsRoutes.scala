package org.ergoplatform.dex.markets.api.v1.routes

import cats.Monad
import cats.effect.{Concurrent, ContextShift, Timer}
import cats.syntax.semigroupk._
import org.ergoplatform.common.http.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.common.http.HttpError
import org.ergoplatform.common.http.syntax._
import org.ergoplatform.dex.markets.api.v1.endpoints.AmmStatsEndpoints
import org.ergoplatform.dex.markets.api.v1.services.{AmmStats, LqLocks}
import org.ergoplatform.dex.markets.configs.RequestConfig
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}
import tofu.Throws
import cats.syntax.option._

final class AmmStatsRoutes[
  F[_]: Concurrent: ContextShift: Timer: AdaptThrowableEitherT[*[_], HttpError]: Throws: Monad
](stats: AmmStats[F], locks: LqLocks[F], requestConf: RequestConfig)(implicit
  opts: Http4sServerOptions[F, F]
) {

  private val endpoints = new AmmStatsEndpoints(requestConf)
  import endpoints._

  private val interpreter = Http4sServerInterpreter(opts)

  def routes: HttpRoutes[F] =
    getLqProviderInfoR <+> checkIfBetaTesterR <+> checkEarlyOffChainR <+>
      checkOffChainR <+> checkEarlyOffChainChartsR <+> checkOffChainChartsR <+> getSwapsStatsR


  def getLqProviderInfoWithOperationsR: HttpRoutes[F] = interpreter.toRoutes(getLqProviderInfoWithOperationsRE) {
    case (address, pool, month, year) =>
      val (from, to) = year match {
        case 2021 => month.from21 -> month.to21
        case 2022 => month.from22 -> month.to22
      }

      stats.getLqProviderInfoWithOperations(address, pool, from, to).adaptThrowable.value
  }

  def getPoolStatsR: HttpRoutes[F] = interpreter.toRoutes(getPoolStats) { case (poolId, tw) =>
    stats.getPoolStats(poolId, tw).adaptThrowable.orNotFound(s"PoolStats{poolId=$poolId}").value
  }

  def getPoolsStatsR: HttpRoutes[F] = interpreter.toRoutes(getPoolsStats) { tw =>
    stats.getPoolsStats(tw).adaptThrowable.value
  def getLqProviderInfoR: HttpRoutes[F] = interpreter.toRoutes(getLqProviderInfoE) { address =>
    stats.getLqProviderInfo(address).adaptThrowable.value
  }

  def getPoolsSummaryR: HttpRoutes[F] = interpreter.toRoutes(getPoolsSummary) { _ =>
    stats.getPoolsSummary.adaptThrowable.value
  def checkIfBetaTesterR: HttpRoutes[F] = interpreter.toRoutes(checkIfBettaTesterE) { case (address) =>
    stats.checkIfBetaTester(address).adaptThrowable.orNotFound(s"BetaTester{addr=$address}").value
  }

  def checkEarlyOffChainR: HttpRoutes[F] = interpreter.toRoutes(getEarlyOffChainOperatorsState) { case (address) =>
    val from = 1628640000000L
    val to   = Some(1636502400000L)
    stats.getOffChainReward(address, from, to, 2500, none).adaptThrowable.value
  }

  def checkOffChainR: HttpRoutes[F] = interpreter.toRoutes(getOffChainOperatorsState) { case (address) =>
    val from = 1636502400000L
    stats.getOffChainReward(address, from, None, 1500, none).adaptThrowable.value
  }

  def checkEarlyOffChainChartsR: HttpRoutes[F] = interpreter.toRoutes(getEarlyOffChainOperatorsStateCharts) { _ =>
    val from = 1628640000000L
    val to   = Some(1636502400000L)
    stats.getOffChainRewardsForAllAddresses(from, to, 2500).adaptThrowable.value
  }

  def checkOffChainChartsR: HttpRoutes[F] = interpreter.toRoutes(getOffChainOperatorsStateCharts) { _ =>
    val from = 1636502400000L
    stats.getOffChainRewardsForAllAddresses(from, None, 1500).adaptThrowable.value
  }

  def getSwapsStatsR: HttpRoutes[F] = interpreter.toRoutes(getSwapsStats) { address =>
    stats.getSwapInfo(address).adaptThrowable.orNotFound(s"SwapsStats{address=$address}").value
  }
}

object AmmStatsRoutes {

  def make[
    F[_]: Concurrent: ContextShift: Timer: AdaptThrowableEitherT[*[_], HttpError]
  ](requestConf: RequestConfig)(implicit
    stats: AmmStats[F],
    locks: LqLocks[F],
    opts: Http4sServerOptions[F, F]
  ): HttpRoutes[F] =
    new AmmStatsRoutes[F](stats, locks, requestConf).routes
}
