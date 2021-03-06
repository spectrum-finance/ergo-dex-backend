package org.ergoplatform.dex.markets.repositories

import cats.data.NonEmptyList
import cats.tagless.syntax.functorK._
import cats.{Apply, FlatMap, Functor}
import derevo.derive
import doobie.ConnectionIO
import doobie.util.log.LogHandler
import org.ergoplatform.dex.markets.models.Trade
import org.ergoplatform.dex.markets.sql.tradesSql
import tofu.doobie.LiftConnectionIO
import tofu.doobie.log.EmbeddableLogHandler
import tofu.higherKind.Mid
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._

@derive(representableK)
trait TradesRepo[F[_]] {

  def insert(trade: Trade): F[Unit]

  def insert(trades: NonEmptyList[Trade]): F[Unit]

  def countTransactions: F[Int]
}

object TradesRepo {

  def make[I[_]: Functor, D[_]: FlatMap: LiftConnectionIO](implicit
    elh: EmbeddableLogHandler[D],
    logs: Logs[I, D]
  ): I[TradesRepo[D]] =
    logs.forService[TradesRepo[D]].map { implicit l =>
      elh.embed(implicit lh => new TradesRepoTracing[D] attach new Live().mapK(LiftConnectionIO[D].liftF))
    }

  final class Live(implicit lh: LogHandler) extends TradesRepo[ConnectionIO] {

    def insert(trade: Trade): ConnectionIO[Unit] =
      tradesSql.insert[Trade].run(trade).map(_ => ())

    def insert(trades: NonEmptyList[Trade]): ConnectionIO[Unit] =
      tradesSql.insert[Trade].updateMany(trades).map(_ => ())

    def countTransactions: ConnectionIO[Int] =
      tradesSql.countTransactions.unique
  }

  final class TradesRepoTracing[F[_]: Apply: Logging] extends TradesRepo[Mid[F, *]] {

    def insert(trade: Trade): Mid[F, Unit] =
      _ <* trace"insert(trade=$trade)"

    def insert(trades: NonEmptyList[Trade]): Mid[F, Unit] =
      _ <* trace"insert(trades=$trades)"

    def countTransactions: Mid[F, Int] =
      _ <* trace"countTransactions"
  }
}
