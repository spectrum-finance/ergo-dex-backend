package org.ergoplatform.dex.index.processes

import cats.data.NonEmptyList
import cats.{Functor, Monad}
import org.ergoplatform.dex.index.repositories.LiquidityProvidersRepo
import tofu.syntax.monadic._
import tofu.syntax.logging._
import cats.syntax.traverse._
import org.ergoplatform.dex.index.db.models.{DBSwapsState, SwapAvg}
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
      sql.selectAllSwapUsers
        .flatMap { users =>
//          users
//            .map { key =>
//              sql.getDaysOfSwapsByAddress(key).flatMap { dates =>
//                val grouped: Map[String, List[SwapAvg]] = dates.groupBy { avg =>
//                  avg.date.take(7)
//                }
//                info"Grouped dates are: $grouped".flatMap { _ =>
//                  val statePerMonthV: List[(BigDecimal, BigDecimal)] =
//                    grouped.values.toList.map { daysInMonth: List[SwapAvg] =>
//                      val m = daysInMonth.length / BigDecimal(30)
//                      val s = daysInMonth.map(_.sum).sum
//
//                      m -> s
//                    }
//
//                  val avgUse = (statePerMonthV.map(_._1).sum / 12).setScale(6, RoundingMode.HALF_UP)
//                  val avgAmount =
//                    (statePerMonthV.map(_._2).sum / BigDecimal(10).pow(9)).setScale(9, RoundingMode.HALF_UP)
//                  val weight = avgUse * avgAmount
//                  info"avgUse: $avgUse, avgAmount: $avgAmount, weight: $weight" >> sql.insert2(
//                    NonEmptyList.one(DBSwapsState(key, avgUse, avgAmount, weight))
//                  )
//                }
//              }
//            }
//            .sequence
//            .as(users)
          users.pure
        }
        .flatMap { users =>
          sql.getTotalErg.flatMap { total =>
            users.map { key =>
              sql.getErgByUser(key).flatMap { userBalance =>
                val res = userBalance / total
//                val r1 = (200 * 18882 * user.weight / total).setScale(6, RoundingMode.HALF_UP)
                sql.update2(key, res)
              }
            }.sequence
          }.void
        }
  }
}
