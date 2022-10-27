package org.ergoplatform.dex.index.processes

import cats.{Functor, Monad}
import org.ergoplatform.dex.index.repositories.LiquidityProvidersRepo
import tofu.syntax.monadic._
import tofu.syntax.logging._
import cats.syntax.traverse._
import org.ergoplatform.dex.index.db.models.SwapAvg
import tofu.logging.{Logging, Logs}

import scala.math.BigDecimal.RoundingMode

trait SwapsStateRunner[F[_]] {
  def run: F[Unit]
}

object SwapsStateRunner {

  def make[I[_]: Functor, F[_]: Monad](implicit
    logs: Logs[I, F],
    sql: LiquidityProvidersRepo[F]
  ): I[SwapsStateRunner[F]] =
    logs.forService[SwapsStateRunner[F]].map(implicit __ => new Impl[F])

  final private class Impl[F[_]: Monad: Logging](implicit sql: LiquidityProvidersRepo[F]) extends SwapsStateRunner[F] {

    def run: F[Unit] =
      sql.selectAllSwapUsers.flatMap { users =>
        users.map { key =>
          sql.getDaysOfSwapsByAddress(key).flatMap { dates =>
            val grouped = dates.groupBy { avg =>
              avg.date.take(7)
            }
            info"Grouped dates are: $grouped".flatMap { _ =>
              val statePerMonthV: List[(BigDecimal, BigDecimal)] = grouped.values.toList.map { v: List[SwapAvg] =>
                val m = v.length / BigDecimal(30)
                val s = v.map(_.sum).sum

                m -> s
              }

              val avgUse    = statePerMonthV.map(_._1).sum.setScale(6, RoundingMode.HALF_UP)
              val avgAmount = (statePerMonthV.map(_._2).sum / BigDecimal(10).pow(9)).setScale(9, RoundingMode.HALF_UP)

              info"avgUse: $avgUse, avgAmount: $avgAmount"
            }
          }
        }.sequence
      }.void
  }
}
