package org.ergoplatform.ergo.services.explorer.models

import derevo.circe.decoder
import derevo.derive

@derive(decoder)
final case class FullBlockInfo(
  header: HeaderInfo,
  blockTransactions: List[TransactionInfo]
)
