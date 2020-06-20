package org.ergoplatform.dex.matcher.repositories

import derevo.derive
import org.ergoplatform.dex.PairId
import org.ergoplatform.dex.domain.models.Order.{AnyOrder, BuyOrder, SellOrder}
import tofu.higherKind.derived.representableK

@derive(representableK)
trait OrdersRepo[D[_]] {

  def getBuyWall(pairId: PairId, limit: Long): D[List[BuyOrder]]

  def getSellWall(pairId: PairId, limit: Long): D[List[SellOrder]]

  def insert(orders: List[AnyOrder]): D[Unit]

  def remove(ids: List[Long]): D[Unit]
}
