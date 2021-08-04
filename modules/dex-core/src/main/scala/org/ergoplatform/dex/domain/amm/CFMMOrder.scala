package org.ergoplatform.dex.domain.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.ergo.models.Output
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
sealed trait CFMMOrder {
  val poolId: PoolId
  val box: Output
  val timestamp: Long

  def id: OrderId = OrderId.fromBoxId(box.boxId)
}

@derive(encoder, decoder, loggable)
final case class Deposit(poolId: PoolId, timestamp: Long, params: DepositParams, box: Output) extends CFMMOrder

@derive(encoder, decoder, loggable)
final case class Redeem(poolId: PoolId, timestamp: Long, params: RedeemParams, box: Output) extends CFMMOrder

@derive(encoder, decoder, loggable)
final case class Swap(poolId: PoolId, timestamp: Long, params: SwapParams, box: Output) extends CFMMOrder
