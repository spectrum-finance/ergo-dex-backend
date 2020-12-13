package org.ergoplatform.dex.explorer.models

import derevo.circe.decoder
import derevo.derive
import io.circe.Json
import org.ergoplatform.dex.{Address, BoxId, HexString, TxId}

@derive(decoder)
final case class Output(
  boxId: BoxId,
  transactionId: TxId,
  value: Long,
  index: Int,
  creationHeight: Int,
  ergoTree: HexString,
  address: Option[Address],
  assets: List[Asset],
  additionalRegisters: Json,
  spentTransactionId: Option[TxId],
  mainChain: Boolean
)
