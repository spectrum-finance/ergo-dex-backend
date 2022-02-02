package org.ergoplatform.ergo.services.node.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.ergo.{BoxId, SErgoTree, TxId}
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
final case class Output(
  boxId: BoxId,
  transactionId: TxId,
  value: Long,
  index: Int,
  creationHeight: Int,
  ergoTree: SErgoTree,
  assets: List[BoxAsset]
)
