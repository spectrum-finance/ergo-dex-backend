package org.ergoplatform.dex.domain

import cats.FlatMap
import org.ergoplatform.network.ErgoNetwork
import org.ergoplatform.network.models.NetworkParams
import tofu.syntax.monadic._

final case class NetworkContext(currentHeight: Int, params: NetworkParams)

object NetworkContext {

  def make[F[_]: FlatMap](implicit network: ErgoNetwork[F]): F[NetworkContext] =
    for {
      height <- network.getCurrentHeight
      params <- network.getNetworkParams
    } yield NetworkContext(height, params)
}