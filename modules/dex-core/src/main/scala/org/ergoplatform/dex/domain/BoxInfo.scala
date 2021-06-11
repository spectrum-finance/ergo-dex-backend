package org.ergoplatform.dex.domain

import org.ergoplatform.ergo.BoxId
import org.ergoplatform.ergo.models.ErgoBox

final case class BoxInfo(boxId: BoxId, value: Long)

object BoxInfo {
  def fromBox(box: ErgoBox): BoxInfo =
    BoxInfo(box.boxId, box.value)
}
