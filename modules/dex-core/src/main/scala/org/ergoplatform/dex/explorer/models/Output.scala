package org.ergoplatform.dex.explorer.models

import derevo.circe.decoder
import derevo.derive
import org.ergoplatform.dex.{Address, BoxId, HexString, TxId}
import tofu.logging.derivation.loggable

@derive(decoder, loggable)
final case class Output(
  boxId: BoxId,
  transactionId: TxId,
  value: Long,
  index: Int,
  creationHeight: Int,
  ergoTree: HexString,
  address: Option[Address],
  assets: List[Asset],
  spentTransactionId: Option[TxId],
  mainChain: Boolean
)
