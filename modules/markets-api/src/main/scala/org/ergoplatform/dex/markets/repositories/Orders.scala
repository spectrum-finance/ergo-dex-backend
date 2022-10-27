package org.ergoplatform.dex.markets.repositories

import cats.tagless.syntax.functorK._
import cats.{FlatMap, Functor}
import derevo.derive
import doobie.ConnectionIO
import doobie.util.query.Query0
import org.ergoplatform.common.models.TimeWindow
import org.ergoplatform.dex.markets.db.models.amm._
import org.ergoplatform.dex.markets.db.sql.AnalyticsSql
import scorex.crypto.authds.merkle.sparse.BlockchainSimulator.PubKey
import tofu.doobie.LiftConnectionIO
import tofu.doobie.log.EmbeddableLogHandler
import tofu.higherKind.Mid
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._

@derive(representableK)
trait Orders[F[_]] {

  def getSwapTxs(tw: TimeWindow): F[List[SwapInfo]]

  def getDepositTxs(tw: TimeWindow): F[List[DepositInfo]]

  def getLqProviderStates(address: String, pool: String, from: Long, to: Long): F[List[DBLpState]]

  def getLqProviderState(address: String, pool: String): F[Option[LqProviderStateDB]]

  def getTotalWeight: F[BigDecimal]

  def getLqUsers: F[Int]

  def getSwapUsersCount: F[Int]

  def checkIfBetaTester(key: org.ergoplatform.ergo.PubKey): F[Int]

  def getSwapsState(key: org.ergoplatform.ergo.PubKey): F[List[SwapState]]

  def getOffChainState(address: String, from: Long, to: Option[Long]): F[Option[OffChainOperatorState]]

  def getTotalOffChainOperationsCount(from: Long, to: Option[Long]): F[Int]

  def getAllOffChainAddresses(from: Long, to: Option[Long]): F[List[String]]

  def getOffChainParticipantsCount(from: Long, to: Option[Long]): F[Int]

  def getAssetTicket: F[List[AssetTicket]]

  def getUserSwapData(key: org.ergoplatform.ergo.PubKey): F[Option[SwapStateUser]]

  def getSummary: F[SwapStateSummary]
}

object Orders {

  def make[I[_]: Functor, D[_]: FlatMap: LiftConnectionIO](implicit
    elh: EmbeddableLogHandler[D],
    logs: Logs[I, D]
  ): I[Orders[D]] =
    logs.forService[Orders[D]].map { implicit l =>
      elh.embed(implicit lh => new OrdersTracing[D] attach new Live(new AnalyticsSql()).mapK(LiftConnectionIO[D].liftF))
    }

  final class Live(sql: AnalyticsSql) extends Orders[ConnectionIO] {

    def getUserSwapData(key: org.ergoplatform.ergo.PubKey): ConnectionIO[Option[SwapStateUser]] =
      sql.getUserSwapData(key).option

    def getSummary: ConnectionIO[SwapStateSummary] =
      sql.getSummary.unique

    def getAssetTicket: ConnectionIO[List[AssetTicket]] =
      sql.getAssetTicket.to[List]

    def getOffChainParticipantsCount(from: Long, to: Option[Long]): ConnectionIO[Int] =
      sql.getOffChainParticipantsCount(from, to).unique

    def getAllOffChainAddresses(from: Long, to: Option[Long]): ConnectionIO[List[String]] =
      sql.getAllOffChainAddresses(from, to).to[List]

    def getOffChainState(address: String, from: Long, to: Option[Long]): ConnectionIO[Option[OffChainOperatorState]] =
      sql.getOffChainState(address, from, to).option

    def getTotalOffChainOperationsCount(from: Long, to: Option[Long]): ConnectionIO[Int] =
      sql.getTotalOffChainOperationsCount(from, to).unique

    def checkIfBetaTester(key: org.ergoplatform.ergo.PubKey): ConnectionIO[Int] =
      sql.checkIfBetaTester(key).unique

    def getSwapsState(key: org.ergoplatform.ergo.PubKey): ConnectionIO[List[SwapState]] =
      sql.getSwapsState(key).to[List]

    def getSwapUsersCount: ConnectionIO[Int] =
      sql.getSwapUsersCount.unique

    def getTotalWeight: ConnectionIO[BigDecimal] =
      sql.getTotalWeight.option.map(_.getOrElse(BigDecimal(0)))

    def getLqUsers: ConnectionIO[Int] =
      sql.getLqUsers.unique

    def getLqProviderStates(address: String, pool: String, from: Long, to: Long): ConnectionIO[List[DBLpState]] =
      sql.getLqProviderStates(address, pool, from, to).to[List]

    def getLqProviderState(address: String, pool: String): ConnectionIO[Option[LqProviderStateDB]] =
      sql.getLqProviderState(address, pool).option

    def getSwapTxs(tw: TimeWindow): ConnectionIO[List[SwapInfo]] =
      sql.getSwapTransactions(tw).to[List]

    def getDepositTxs(tw: TimeWindow): ConnectionIO[List[DepositInfo]] =
      sql.getDepositTransactions(tw).to[List]

  }

  final class OrdersTracing[F[_]: FlatMap: Logging] extends Orders[Mid[F, *]] {

    def getUserSwapData(key: org.ergoplatform.ergo.PubKey) =
      for {
        _ <- trace"getUserSwapData($key)"
        r <- _
        _ <- trace"getUserSwapData($key) -> $r res"
      } yield r

    def getSummary =
      for {
        _ <- trace"getSummary()"
        r <- _
        _ <- trace"getSummary() -> $r res"
      } yield r

    def getAssetTicket =
      for {
        _ <- trace"getAssetTicket()"
        r <- _
        _ <- trace"getAssetTicket() -> $r res"
      } yield r

    def getOffChainParticipantsCount(from: Long, to: Option[Long]) =
      for {
        _ <- trace"getOffChainParticipantsCount($from, $to)"
        r <- _
        _ <- trace"getOffChainParticipantsCount($from, $to) -> $r res"
      } yield r

    def getAllOffChainAddresses(from: Long, to: Option[Long]) =
      for {
        _ <- trace"getAllOffChainAddresses($from, $to)"
        r <- _
        _ <- trace"getAllOffChainAddresses($from, $to) -> $r res"
      } yield r

    def getOffChainState(address: String, from: Long, to: Option[Long]) =
      for {
        _ <- trace"getOffChainState($address, $from, $to)"
        r <- _
        _ <- trace"getOffChainState($address, $from, $to) -> $r res"
      } yield r

    def getTotalOffChainOperationsCount(from: Long, to: Option[Long]) =
      for {
        _ <- trace"getTotalOffChainOperationsCount($from, $to)"
        r <- _
        _ <- trace"getTotalOffChainOperationsCount($from, $to) -> $r res"
      } yield r

    def checkIfBetaTester(key: org.ergoplatform.ergo.PubKey) =
      for {
        _ <- trace"checkIfBetaTester($key)"
        r <- _
        _ <- trace"checkIfBetaTester($key) -> $r res"
      } yield r

    def getSwapsState(key: org.ergoplatform.ergo.PubKey) =
      for {
        _ <- trace"getSwapsState($key)"
        r <- _
        _ <- trace"getSwapsState($key) -> $r res"
      } yield r

    def getSwapUsersCount =
      for {
        _ <- trace"getSwapUsersCount()"
        r <- _
        _ <- trace"getSwapUsersCount() -> $r res"
      } yield r

    def getLqUsers: Mid[F, Int] =
      for {
        _ <- trace"getLqUsers()"
        r <- _
        _ <- trace"getLqUsers() -> $r res"
      } yield r

    def getTotalWeight: Mid[F, BigDecimal] =
      for {
        _ <- trace"getTotalWeight()"
        r <- _
        _ <- trace"getTotalWeight() -> $r res"
      } yield r

    def getLqProviderState(address: String, pool: String) =
      for {
        _ <- trace"getLqProviderStates(address=$address, pool=$pool)"
        r <- _
        _ <- trace"getLqProviderStates(address=$address, pool=$pool) -> $r res"
      } yield r

    def getLqProviderStates(address: String, pool: String, from: Long, to: Long) =
      for {
        _ <- trace"getStateByAddressAndPool(address=$address, pool=$pool from=$from, to=$to)"
        r <- _
        _ <- trace"getStateByAddressAndPool(address=$address, pool=$pool from=$from, to=$to) -> ${r.size} res"
      } yield r

    def getSwapTxs(tw: TimeWindow): Mid[F, List[SwapInfo]] =
      for {
        _ <- trace"swaps(window=$tw)"
        r <- _
        _ <- trace"swaps(window=$tw) -> ${r.size} swap entities selected"
      } yield r

    def getDepositTxs(tw: TimeWindow): Mid[F, List[DepositInfo]] =
      for {
        _ <- trace"deposits(window=$tw)"
        r <- _
        _ <- trace"deposits(window=$tw) -> ${r.size} deposit entities selected"
      } yield r
  }
}
