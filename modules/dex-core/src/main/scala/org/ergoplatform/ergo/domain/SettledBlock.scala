package org.ergoplatform.ergo.domain

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.ergo.services.explorer.models.BlockInfo
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
final case class SettledBlock(
  id: String,
  height: Int,
  timestamp: Long
)

object SettledBlock {

  def fromExplorer(block: BlockInfo): SettledBlock =
    SettledBlock(
      block.id,
      block.height,
      block.timestamp
    )
}
