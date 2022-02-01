package org.ergoplatform.ergo.services.explorer.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.ergo.domain.{RegisterId, SConstant}
import org.ergoplatform.ergo.{Address, BoxId, SErgoTree, TxId}
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
final case class Output(
  boxId: BoxId,
  transactionId: TxId,
  value: Long,
  index: Int,
  globalIndex: Long,
  creationHeight: Int,
  settlementHeight: Int,
  ergoTree: SErgoTree,
  address: Address,
  assets: List[BoxAsset],
  additionalRegisters: Map[RegisterId, SConstant]
)
