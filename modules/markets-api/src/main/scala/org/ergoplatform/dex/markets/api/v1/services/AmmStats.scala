package org.ergoplatform.dex.markets.api.v1.services

import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.dex.markets.api.v1.models.amm.{PlatformSummary, PoolSummary}

import scala.concurrent.duration.FiniteDuration

trait AmmStats[F[_]] {

  def getPlatformSummary(tail: FiniteDuration): F[PlatformSummary]

  def getPoolSummary(poolId: PoolId, tail: FiniteDuration): F[Option[PoolSummary]]
}
