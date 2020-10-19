package org.ergoplatform.dex.matcher.repositories

import derevo.derive
import org.ergoplatform.dex.{OrderId, PairId}
import org.ergoplatform.dex.domain.models.Order.{AnyOrder, Ask, Bid}
import tofu.higherKind.derived.representableK

@derive(representableK)
trait OrdersRepo[D[_]] {

  def getBuyWall(pairId: PairId, limit: Long): D[List[Bid]]

  def getSellWall(pairId: PairId, limit: Long): D[List[Ask]]

  def insert(orders: List[AnyOrder]): D[Unit]

  def remove(ids: List[OrderId]): D[Unit]
}
