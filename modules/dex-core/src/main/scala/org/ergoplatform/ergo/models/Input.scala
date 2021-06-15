package org.ergoplatform.ergo.models

import derevo.circe.decoder
import derevo.derive
import org.ergoplatform.common.HexString
import org.ergoplatform.ergo._
import tofu.logging.derivation.loggable

@derive(decoder, loggable)
final case class Input(
  boxId: BoxId,
  value: Long,
  index: Int,
  spendingProof: Option[HexString],
  outputTransactionId: TxId,
  outputIndex: Int,
  outputGlobalIndex: Long,
  ergoTree: SErgoTree,
  address: Address,
  assets: List[BoxAsset]
) extends ErgoBox
