package org.ergoplatform.ergo.services.node.models

import org.ergoplatform.ergo.{BoxId, SErgoTree, TxId}

final case class Output(
  boxId: BoxId,
  transactionId: TxId,
  value: Long,
  index: Int,
  creationHeight: Int,
  ergoTree: SErgoTree,
  assets: List[BoxAsset]
)
