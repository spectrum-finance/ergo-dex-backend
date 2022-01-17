package org.ergoplatform.dex.markets.modules

import cats.Monad
import cats.effect.Clock
import org.ergoplatform.dex.markets.db.models.PoolInfo
import org.ergoplatform.dex.markets.domain.{FeePercentProjection, Fees, TotalValueLocked}
import tofu.syntax.time.now._
import tofu.syntax.monadic._

import scala.concurrent.duration.FiniteDuration
import scala.math.BigDecimal.RoundingMode

trait AmmStatsMath[F[_]] {

  def feePercentProjection(
    tvl: TotalValueLocked,
    fees: Fees,
    poolInfo: PoolInfo,
    projectionPeriod: FiniteDuration
  ): F[FeePercentProjection]
}

object AmmStatsMath {

  implicit def instance[F[_]: Monad: Clock]: AmmStatsMath[F] =
    new AmmStatsMath[F] {

      def feePercentProjection(
        tvl: TotalValueLocked,
        fees: Fees,
        poolInfo: PoolInfo,
        projectionPeriod: FiniteDuration
      ): F[FeePercentProjection] =
        for {
          windowSizeMillis <-
            for {
              ub <- fees.window.to.fold(millis[F])(_.pure[F])
              lb = fees.window.from.getOrElse(poolInfo.confirmedAt)
            } yield ub - lb
          periodFees        = fees.value * (BigDecimal(projectionPeriod.toMillis) / windowSizeMillis)
          periodFeesPercent = (periodFees * 100 / tvl.value).setScale(2, RoundingMode.HALF_UP).toDouble
        } yield FeePercentProjection(periodFeesPercent)
    }
}
