package org.ergoplatform.ergo.models

import org.ergoplatform.ergo.{BoxId, SErgoTree}
import tofu.logging.Loggable

trait ErgoBox {
  val boxId: BoxId
  val value: Long
  val ergoTree: SErgoTree
  val assets: List[BoxAsset]
  val additionalRegisters: Map[RegisterId, SConstant]
}

object ErgoBox {

  implicit val regsLoggable: Loggable[Map[RegisterId, SConstant]] =
    Loggable.stringValue.contramap { x =>
      x.toString()
    }
}
