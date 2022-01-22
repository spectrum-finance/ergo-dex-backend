package org.ergoplatform.dex.protocol.locks

import org.ergoplatform.dex.protocol.ProtoVer
import org.ergoplatform.ergo.ErgoTreeTemplate

trait LiquidityLockTemplates[V <: ProtoVer] {
  def lock: ErgoTreeTemplate
}

object LiquidityLockTemplates {

  implicit object ImplV0 extends LiquidityLockTemplates[ProtoVer.V0] {
    def lock: ErgoTreeTemplate = ???
  }
}
