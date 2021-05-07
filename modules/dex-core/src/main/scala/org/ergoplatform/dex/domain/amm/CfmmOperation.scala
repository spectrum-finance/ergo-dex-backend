package org.ergoplatform.dex.domain.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.dex.domain.network.Output
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
sealed trait CfmmOperation {
  val box: Output

  def id: OperationId = OperationId.fromBoxId(box.boxId)
}

@derive(encoder, decoder, loggable)
final case class Deposit(params: DepositParams, box: Output) extends CfmmOperation

@derive(encoder, decoder, loggable)
final case class Redeem(params: RedeemParams, box: Output) extends CfmmOperation

@derive(encoder, decoder, loggable)
final case class Swap(params: SwapParams, box: Output) extends CfmmOperation
