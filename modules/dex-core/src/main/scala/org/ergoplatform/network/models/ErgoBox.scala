package org.ergoplatform.network.models

import org.ergoplatform.dex.{BoxId, SErgoTree}

trait ErgoBox {
  val boxId: BoxId
  val value: Long
  val ergoTree: SErgoTree
  val assets: List[BoxAsset]
}
