package org.ergoplatform.dex.domain.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.ergo.models.Output
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
sealed trait CFMMOperationRequest {
  val poolId: PoolId
  val box: Output

  def id: OperationId = OperationId.fromBoxId(box.boxId)
}

@derive(encoder, decoder, loggable)
final case class Deposit(poolId: PoolId, params: DepositParams, box: Output) extends CFMMOperationRequest

@derive(encoder, decoder, loggable)
final case class Redeem(poolId: PoolId, params: RedeemParams, box: Output) extends CFMMOperationRequest

@derive(encoder, decoder, loggable)
final case class Swap(poolId: PoolId, params: SwapParams, box: Output) extends CFMMOperationRequest
