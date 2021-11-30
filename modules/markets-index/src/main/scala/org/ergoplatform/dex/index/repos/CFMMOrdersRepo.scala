package org.ergoplatform.dex.index.repos

import cats.data.NonEmptyList
import cats.{FlatMap, Functor}
import doobie.ConnectionIO
import doobie.util.log.LogHandler
import org.ergoplatform.dex.index.db.models.{DBDeposit, DBRedeem, DBSwap}
import org.ergoplatform.dex.index.sql.{DepositOrdersSql, RedeemOrdersSql, SwapOrdersSql}
import tofu.doobie.LiftConnectionIO
import tofu.doobie.log.EmbeddableLogHandler
import tofu.logging.Logs
import tofu.syntax.monadic._
import cats.tagless.syntax.functorK._
import tofu.higherKind.derived.representableK
import derevo.derive

@derive(representableK)
trait CFMMOrdersRepo[F[_]] {
  def insertSwaps(swaps: NonEmptyList[DBSwap]): F[Int]
  def insertDeposits(deposits: NonEmptyList[DBDeposit]): F[Int]
  def insertRedeems(redeems: NonEmptyList[DBRedeem]): F[Int]
}

object CFMMOrdersRepo {

  def make[I[_]: Functor, D[_]: FlatMap: LiftConnectionIO](implicit
    elh: EmbeddableLogHandler[D],
    logs: Logs[I, D]
  ): I[CFMMOrdersRepo[D]] =
    logs.forService[CFMMOrdersRepo[D]].map { implicit l =>
      elh.embed(implicit lh => new Live().mapK(LiftConnectionIO[D].liftF))
    }

  final class Live(implicit lh: LogHandler) extends CFMMOrdersRepo[ConnectionIO] {

    def insertSwaps(swaps: NonEmptyList[DBSwap]): ConnectionIO[Int] =
      SwapOrdersSql.insert[DBSwap].updateMany(swaps)

    def insertRedeems(redeems: NonEmptyList[DBRedeem]): ConnectionIO[Int] =
      RedeemOrdersSql.insert[DBRedeem].updateMany(redeems)

    def insertDeposits(deposits: NonEmptyList[DBDeposit]): ConnectionIO[Int] =
      DepositOrdersSql.insert[DBDeposit].updateMany(deposits)
  }
}
