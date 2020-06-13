package org.ergoplatform.dex.matcher.repositories

import derevo.derive
import derevo.tagless.functorK
import org.ergoplatform.dex.PairId
import org.ergoplatform.dex.domain.models.Order.{BuyOrder, SellOrder}

@derive(functorK)
trait OrdersRepo[D[_]] {

  def getBuyWall(pairId: PairId, limit: Long): D[List[BuyOrder]]

  def getSellWall(pairId: PairId, limit: Long): D[List[SellOrder]]

  def remove(ids: List[Long]): D[Unit]
}
