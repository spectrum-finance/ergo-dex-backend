package org.ergoplatform.dex.clients.explorer.models

import derevo.circe.decoder
import derevo.derive

@derive(decoder)
final case class BlockInfo(
  id: String,
  height: Int,
  timestamp: Long,
  transactionsCount: Int,
  size: Int,
  difficulty: Long,
  minerReward: Long
)
