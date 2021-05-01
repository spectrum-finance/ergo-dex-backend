package org.ergoplatform.dex.executor.amm.repositories

import cats.Functor
import org.ergoplatform.dex.clients.ErgoNetwork
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.dex.domain.network.Output
import tofu.syntax.monadic._

trait Pools[F[_]] {

  def get(id: PoolId): F[Option[Output]]
}

object Pools {

  def make[F[_]: Functor](implicit network: ErgoNetwork[F]): Pools[F] =
    new Live[F]

  final class Live[F[_]: Functor](implicit network: ErgoNetwork[F]) extends Pools[F] {

    def get(id: PoolId): F[Option[Output]] =
      network.getUtxoByToken(id.value, offset = 0, limit = 1).map(_.headOption)
  }
}
