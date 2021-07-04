package org.ergoplatform.dex.domain

import cats.FlatMap
import org.ergoplatform.ergo.ErgoNetwork
import org.ergoplatform.ergo.models.EpochParams
import tofu.syntax.monadic._

final case class NetworkContext(currentHeight: Int, params: EpochParams)

object NetworkContext {

  def make[F[_]: FlatMap](implicit network: ErgoNetwork[F]): F[NetworkContext] =
    for {
      height <- network.getCurrentHeight
      params <- network.getEpochParams
    } yield NetworkContext(height, params)
}