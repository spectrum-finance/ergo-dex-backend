package org.ergoplatform.dex.markets.services

import cats.effect.Clock
import cats.tagless.syntax.functorK._
import cats.{FlatMap, Functor, Monad}
import derevo.derive
import org.ergoplatform.common.caching.Memoize
import org.ergoplatform.dex.domain.Market
import org.ergoplatform.dex.markets.db.models.amm.PoolSnapshot
import org.ergoplatform.dex.markets.repositories.Pools
import org.ergoplatform.ergo.TokenId
import tofu.concurrent.MakeRef
import tofu.doobie.transactor.Txr
import tofu.higherKind.Mid
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.syntax.monadic._
import tofu.syntax.logging._

import scala.concurrent.duration._

@derive(representableK)
trait Markets[F[_]] {

  /** Get all available markets.
    */
  def getAll: F[List[Market]]

  /** Get markets involving an asset with the given id.
    */
  def getByAsset(id: TokenId): F[List[Market]]
}

object Markets {

  def parsePools(pools: List[PoolSnapshot]): List[Market] =
    pools.map(p => Market.fromReserves(p.lockedX, p.lockedY))

  val MemoTtl: FiniteDuration = 4.minutes

  def make[I[_]: FlatMap, F[_]: Monad: Clock, D[_]](implicit
    txr: Txr.Aux[F, D],
    pools: Pools[D],
    makeRef: MakeRef[I, F],
    logs: Logs[I, F]
  ): I[Markets[F]] =
    logs.forService[Markets[F]].flatMap { implicit l =>
      Memoize.make[I, F, List[Market]].map { memo =>
        val poolsF = pools.mapK(txr.trans)
        new MarketsTracing[F] attach (new MarketsMemo(poolsF, memo) attach new Live(poolsF))
      }
    }

  final class Live[F[_]: Monad](pools: Pools[F]) extends Markets[F] {

    def getAll: F[List[Market]] =
      pools.snapshots.map(parsePools)

    def getByAsset(id: TokenId): F[List[Market]] =
      pools.snapshotsByAsset(id).map(parsePools)
  }

  final class MarketsMemo[F[_]: Monad](pools: Pools[F], memo: Memoize[F, List[Market]]) extends Markets[Mid[F, *]] {

    def getAll: Mid[F, List[Market]] =
      fa =>
        for {
          maybeMarkets <- memo.read
          res <- maybeMarkets match {
                   case Some(markets) => markets.pure
                   case None          => fa.flatTap(memo.memoize(_, MemoTtl))
                 }
        } yield res

    def getByAsset(id: TokenId): Mid[F, List[Market]] =
      fa =>
        for {
          maybeMarkets <- memo.read
          res <- maybeMarkets match {
                   case Some(markets) => markets.filter(_.contains(id)).pure
                   case None          => fa.flatTap(_ => pools.snapshots.map(parsePools) >>= (memo.memoize(_, MemoTtl)))
                 }
        } yield res
  }

  final class MarketsTracing[F[_]: Monad: Logging] extends Markets[Mid[F, *]] {

    def getAll: Mid[F, List[Market]] =
      for {
        _ <- trace"getAll()"
        r <- _
        _ <- trace"getAll() -> $r"
      } yield r

    def getByAsset(tokenId: TokenId): Mid[F, List[Market]] =
      for {
        _ <- trace"getByAsset(tokenId=$tokenId)"
        r <- _
        _ <- trace"getByAsset(tokenId=$tokenId) -> $r"
      } yield r
  }
}
