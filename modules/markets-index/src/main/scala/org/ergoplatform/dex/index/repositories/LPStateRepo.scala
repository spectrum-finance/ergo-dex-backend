package org.ergoplatform.dex.index.repositories

import cats.data.NonEmptyList
import cats.{FlatMap, Functor, Monad}
import derevo.derive
import doobie.ConnectionIO
import doobie.util.log.LogHandler
import org.ergoplatform.dex.index.db.models.{DBLpState, PoolStateLatest}
import org.ergoplatform.dex.index.sql.LpStateSql
import tofu.doobie.LiftConnectionIO
import tofu.doobie.log.EmbeddableLogHandler
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.syntax.monadic._
import tofu.syntax.logging._
import cats.tagless.syntax.functorK._
import tofu.doobie.transactor.Txr
import tofu.higherKind.Mid

@derive(representableK)
trait LPStateRepo[F[_]] extends MonoRepo[DBLpState, F] {

  def getPreviousState(address: String, poolLpToken: String): F[Option[DBLpState]]

  def getLatestPoolState(poolLpToken: String, to: Long): F[Option[PoolStateLatest]]
}

object LPStateRepo {

  def make[I[_]: Functor, F[_], D[_]: Monad: LiftConnectionIO](implicit
    elh: EmbeddableLogHandler[D],
    logs: Logs[I, D],
    txr: Txr.Aux[F, D]
  ): I[LPStateRepo[F]] =
    logs.forService[LPStateRepo[D]].map { implicit l =>
      elh
        .embed(implicit lh => new Tracing[D] attach new Live().mapK(LiftConnectionIO[D].liftF))
        .mapK(txr.trans)
    }

  final private class Live(implicit lh: LogHandler) extends LPStateRepo[ConnectionIO] {

    def getPreviousState(address: String, poolLpToken: String): ConnectionIO[Option[DBLpState]] =
      LpStateSql.getPreviousState(address, poolLpToken).option

    def getLatestPoolState(poolLpToken: String, to: Long): ConnectionIO[Option[PoolStateLatest]] =
      LpStateSql.getLatestPoolState(poolLpToken, to).option

    def insert(entities: NonEmptyList[DBLpState]): ConnectionIO[Int] =
      LpStateSql.insertNoConflict.updateMany(entities)
  }

  final private class Tracing[F[_]: Monad: Logging] extends LPStateRepo[Mid[F, *]] {

    def getLatestPoolState(poolLpToken: String, to: Long) =
      for {
        _ <- info"getLatestPoolState($poolLpToken, $to)."
        r <- _
        _ <- info"getLatestPoolState($poolLpToken, $to) -> $r"
      } yield r

    def getPreviousState(address: String, poolLpToken: String): Mid[F, Option[DBLpState]] =
      for {
        _ <- info"Going to get state for address $address and pool $poolLpToken."
        r <- _
        _ <- info"State for address $address and pool $poolLpToken is: $r"
      } yield r

    def insert(entities: NonEmptyList[DBLpState]): Mid[F, Int] =
      trace"Going to insert new state ${entities.toList}" *> _ <* trace"Insert finished."
  }

}
