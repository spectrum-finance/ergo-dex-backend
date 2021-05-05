package org.ergoplatform.dex.domain.network

import derevo.circe.decoder
import derevo.derive
import org.ergoplatform.dex._
import tofu.logging.derivation.loggable

@derive(decoder, loggable)
final case class Input(
  boxId: BoxId,
  value: Long,
  index: Int,
  spendingProof: Option[HexString],
  outputTransactionId: TxId,
  outputIndex: Int,
  ergoTree: SErgoTree,
  address: Address,
  assets: List[BoxAsset]
) extends ErgoBox
