package org.ergoplatform.dex.clients.explorer.models

import derevo.circe.decoder
import derevo.derive
import io.circe.Json
import org.ergoplatform.dex.{Address, BoxId, SErgoTree, TxId}
import tofu.logging.derivation.loggable

@derive(decoder, loggable)
final case class Output(
  boxId: BoxId,
  transactionId: TxId,
  value: Long,
  index: Int,
  creationHeight: Int,
  ergoTree: SErgoTree,
  address: Address,
  assets: List[Asset],
  spentTransactionId: Option[TxId],
  additionalRegisters: Json
)
