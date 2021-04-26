package org.ergoplatform.dex.domain.amm

import org.ergoplatform.dex.clients.explorer.models.Output

sealed trait AmmOperation {
  val box: Output
}

final case class Deposit(box: Output) extends AmmOperation

final case class Redeem(box: Output) extends AmmOperation

final case class Swap(box: Output) extends AmmOperation
