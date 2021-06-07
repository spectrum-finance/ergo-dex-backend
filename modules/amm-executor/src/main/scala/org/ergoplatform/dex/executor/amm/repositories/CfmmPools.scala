package org.ergoplatform.dex.executor.amm.repositories

import cats.Monad
import cats.data.OptionT
import org.ergoplatform.dex.clients.ErgoNetwork
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.amm.{CfmmPool, PoolId}
import org.ergoplatform.network.RegisterId
import org.ergoplatform.network.SConstant.LongConstant
import org.ergoplatform.dex.protocol.amm.constants
import tofu.syntax.monadic._

trait CfmmPools[F[_]] {

  def get(id: PoolId): F[Option[CfmmPool]]
}

object CfmmPools {

  def make[F[_]: Monad](implicit network: ErgoNetwork[F]): CfmmPools[F] =
    new Live[F]

  final class Live[F[_]: Monad](implicit network: ErgoNetwork[F]) extends CfmmPools[F] {

    def get(id: PoolId): F[Option[CfmmPool]] =
      (for {
        poolBox <- OptionT(network.getUtxoByToken(id.value, offset = 0, limit = 1).map(_.headOption))
        lp      <- OptionT.fromOption(poolBox.assets.lift(constants.cfmm.t2t.IndexLP))
        x       <- OptionT.fromOption(poolBox.assets.lift(constants.cfmm.t2t.IndexX))
        y       <- OptionT.fromOption(poolBox.assets.lift(constants.cfmm.t2t.IndexY))
        fee     <- OptionT.fromOption(poolBox.additionalRegisters.get(RegisterId.R4).collect { case LongConstant(x) => x })
      } yield CfmmPool(
        id,
        AssetAmount.fromBoxAsset(lp),
        AssetAmount.fromBoxAsset(x),
        AssetAmount.fromBoxAsset(y),
        fee,
        poolBox
      )).value
  }
}
