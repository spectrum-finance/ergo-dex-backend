package org.ergoplatform.dex.domain

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.ergo.BoxId
import org.ergoplatform.ergo.models.ErgoBox
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
final case class BoxInfo(boxId: BoxId, value: Long)

object BoxInfo {
  def fromBox(box: ErgoBox): BoxInfo =
    BoxInfo(box.boxId, box.value)
}
