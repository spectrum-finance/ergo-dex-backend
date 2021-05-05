package org.ergoplatform.dex.domain.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.network.Output
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
sealed trait CfmmOperation {
  val poolId: PoolId
  val box: Output

  def id: OperationId = OperationId.fromBoxId(box.boxId)
}

@derive(encoder, decoder, loggable)
final case class Deposit(poolId: PoolId, box: Output) extends CfmmOperation

@derive(encoder, decoder, loggable)
final case class Redeem(poolId: PoolId, box: Output) extends CfmmOperation

@derive(encoder, decoder, loggable)
final case class Swap(poolId: PoolId, input: AssetAmount, minOutput: AssetAmount, minerFee: Long, box: Output)
  extends CfmmOperation
