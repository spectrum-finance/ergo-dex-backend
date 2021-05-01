package org.ergoplatform.dex.markets.repositories

import cats.data.NonEmptyList
import cats.tagless.syntax.functorK._
import cats.{Apply, FlatMap, Functor}
import derevo.derive
import doobie.ConnectionIO
import doobie.util.log.LogHandler
import org.ergoplatform.dex.TokenId
import org.ergoplatform.dex.markets.models.Fill
import org.ergoplatform.dex.markets.sql.fillsSql
import tofu.doobie.LiftConnectionIO
import tofu.doobie.log.EmbeddableLogHandler
import tofu.higherKind.Mid
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._

@derive(representableK)
trait FillsRepo[F[_]] {

  def insert(fill: Fill): F[Unit]

  def insert(fills: NonEmptyList[Fill]): F[Unit]

  def volumeByPair(quote: TokenId, base: TokenId)(fromTs: Long): F[Long]

  def countTransactions: F[Int]
}

object FillsRepo {

  def make[I[_]: Functor, D[_]: FlatMap: LiftConnectionIO](implicit
    elh: EmbeddableLogHandler[D],
    logs: Logs[I, D]
  ): I[FillsRepo[D]] =
    logs.forService[FillsRepo[D]].map { implicit l =>
      elh.embed(implicit lh => new FillsRepoTracing[D] attach new Live().mapK(LiftConnectionIO[D].liftF))
    }

  final class Live(implicit lh: LogHandler) extends FillsRepo[ConnectionIO] {

    def insert(trade: Fill): ConnectionIO[Unit] =
      fillsSql.insert[Fill].run(trade).map(_ => ())

    def insert(trades: NonEmptyList[Fill]): ConnectionIO[Unit] =
      fillsSql.insert[Fill].updateMany(trades).map(_ => ())

    def volumeByPair(quote: TokenId, base: TokenId)(fromTs: Long): ConnectionIO[Long] =
      fillsSql.volumeByPair(quote, base)(fromTs).unique

    def countTransactions: ConnectionIO[Int] =
      fillsSql.countTransactions.unique
  }

  final class FillsRepoTracing[F[_]: Apply: Logging] extends FillsRepo[Mid[F, *]] {

    def insert(fill: Fill): Mid[F, Unit] =
      _ <* trace"insert( fill=$fill )"

    def insert(fills: NonEmptyList[Fill]): Mid[F, Unit] =
      _ <* trace"insert( fills=$fills )"

    def volumeByPair(quote: TokenId, base: TokenId)(fromTs: Long): Mid[F, Long] =
      _ <* trace"volumeByPair( quote=$quote, base=$base )( fromTs=$fromTs )"

    def countTransactions: Mid[F, Int] =
      _ <* trace"countTransactions"
  }
}
