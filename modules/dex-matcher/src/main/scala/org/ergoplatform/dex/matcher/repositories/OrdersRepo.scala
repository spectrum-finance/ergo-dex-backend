package org.ergoplatform.dex.matcher.repositories

import derevo.derive
import derevo.tagless.functorK
import org.ergoplatform.dex.PairId
import org.ergoplatform.dex.domain.TaggedOrder.{BuyOrder, SellOrder}

@derive(functorK)
trait OrdersRepo[F[_]] {

  def getBuyWall(pairId: PairId, limit: Long): F[List[BuyOrder]]

  def getSellWall(pairId: PairId, limit: Long): F[List[SellOrder]]
}
