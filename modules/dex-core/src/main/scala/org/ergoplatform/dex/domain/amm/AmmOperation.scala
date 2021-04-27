package org.ergoplatform.dex.domain.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.dex.OperationId
import org.ergoplatform.dex.clients.explorer.models.Output
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
sealed trait AmmOperation {
  val box: Output

  def id: OperationId = OperationId.fromBoxId(box.boxId)
}

@derive(encoder, decoder, loggable)
final case class Deposit(box: Output) extends AmmOperation

@derive(encoder, decoder, loggable)
final case class Redeem(box: Output) extends AmmOperation

@derive(encoder, decoder, loggable)
final case class Swap(box: Output) extends AmmOperation
