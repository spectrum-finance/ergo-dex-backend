package org.ergoplatform.dex.executor.orders.context

import tofu.{Context, WithContext, WithLocal}

final case class BlockchainContext(currentHeight: Int, nanoErgsPerByte: Long)

object BlockchainContext extends WithContext.Companion[BlockchainContext] {
  type Local[F[_]] = F WithLocal BlockchainContext
  def empty: BlockchainContext = BlockchainContext(0, 0L)
}
