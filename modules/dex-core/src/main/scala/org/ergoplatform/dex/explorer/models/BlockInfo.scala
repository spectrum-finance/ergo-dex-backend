package org.ergoplatform.dex.explorer.models

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

final case class BlockInfo(
  id: String,
  height: Int,
  timestamp: Long,
  transactionsCount: Int,
  size: Int,
  difficulty: Long,
  minerReward: Long
)

object BlockInfo {

  implicit val decoder: Decoder[BlockInfo] = deriveDecoder
}
