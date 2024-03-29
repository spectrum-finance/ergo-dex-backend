package org.ergoplatform.ergo.services.explorer.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.common.HexString
import org.ergoplatform.ergo._
import org.ergoplatform.ergo.domain.{RegisterId, SConstant}
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
final case class Input(
  boxId: BoxId,
  value: Long,
  index: Int,
  spendingProof: Option[HexString],
  outputTransactionId: TxId,
  outputIndex: Int,
  outputGlobalIndex: Long,
  outputCreatedAt: Int,
  outputSettledAt: Int,
  ergoTree: SErgoTree,
  address: Address,
  assets: List[BoxAsset],
  additionalRegisters: Map[RegisterId, SConstant]
)
