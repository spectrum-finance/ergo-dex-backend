package org.ergoplatform.ergo.domain

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.ergo.services.explorer.models.BlockInfo
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
final case class Block(
  id: String,
  height: Int,
  timestamp: Long
)

object Block {

  def fromExplorer(block: BlockInfo): Block =
    Block(
      block.id,
      block.height,
      block.timestamp
    )
}
