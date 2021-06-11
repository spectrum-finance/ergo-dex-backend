package org.ergoplatform.ergo.models

import org.ergoplatform.ergo.{BoxId, SErgoTree}

trait ErgoBox {
  val boxId: BoxId
  val value: Long
  val ergoTree: SErgoTree
  val assets: List[BoxAsset]
}
