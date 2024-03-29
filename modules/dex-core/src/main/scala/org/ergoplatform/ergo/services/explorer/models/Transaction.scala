package org.ergoplatform.ergo.services.explorer.models

import derevo.circe.decoder
import derevo.derive
import org.ergoplatform.ergo.{BlockId, TxId}
import tofu.logging.derivation.loggable

@derive(decoder, loggable)
final case class Transaction(
  id: TxId,
  blockId: BlockId,
  inclusionHeight: Int,
  timestamp: Long,
  index: Int,
  globalIndex: Long,
  numConfirmations: Int,
  inputs: List[Input],
  outputs: List[Output]
)
