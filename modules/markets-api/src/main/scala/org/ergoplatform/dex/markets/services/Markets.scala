package org.ergoplatform.dex.markets.services

import cats.Monad
import derevo.derive
import org.ergoplatform.dex.domain.Market
import org.ergoplatform.dex.markets.repositories.Pools
import org.ergoplatform.ergo.TokenId
import tofu.higherKind.derived.representableK
import tofu.syntax.monadic._

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

  final class Live[F[_]: Monad](pools: Pools[F]) extends Markets[F] {

    def getAll: F[List[Market]] =
      pools.snapshots.map { pools =>
        pools.map(p => Market.fromReserves(p.lockedX, p.lockedY))
      }

    def getByAsset(id: TokenId): F[List[Market]] =
      pools.snapshotsByAsset(id).map { pools =>
        pools.map(p => Market.fromReserves(p.lockedX, p.lockedY))
      }
  }
}
