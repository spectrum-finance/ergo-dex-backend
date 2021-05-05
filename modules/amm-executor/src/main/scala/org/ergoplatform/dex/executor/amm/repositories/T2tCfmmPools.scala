package org.ergoplatform.dex.executor.amm.repositories

import cats.data.OptionT
import cats.{Functor, Monad}
import org.ergoplatform.dex.clients.ErgoNetwork
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.amm.{PoolId, T2tCfmmPool}
import org.ergoplatform.dex.protocol.amm.constants
import tofu.syntax.monadic._

trait T2tCfmmPools[F[_]] {

  def get(id: PoolId): F[Option[T2tCfmmPool]]
}

object T2tCfmmPools {

  def make[F[_]: Monad](implicit network: ErgoNetwork[F]): T2tCfmmPools[F] =
    new Live[F]

  final class Live[F[_]: Monad](implicit network: ErgoNetwork[F]) extends T2tCfmmPools[F] {

    def get(id: PoolId): F[Option[T2tCfmmPool]] =
      (for {
        poolBox <- OptionT(network.getUtxoByToken(id.value, offset = 0, limit = 1).map(_.headOption))
        lp      <- OptionT.fromOption(poolBox.assets.lift(constants.cfmm.t2t.IndexLP))
        x       <- OptionT.fromOption(poolBox.assets.lift(constants.cfmm.t2t.IndexX))
        y       <- OptionT.fromOption(poolBox.assets.lift(constants.cfmm.t2t.IndexY))
      } yield T2tCfmmPool(
        id,
        AssetAmount.fromBoxAsset(lp),
        AssetAmount.fromBoxAsset(x),
        AssetAmount.fromBoxAsset(y),
        ???,
        poolBox
      )).value
  }
}
