package org.ergoplatform.dex.domain.models

import org.ergoplatform.dex.BoxId
import sigmastate.Values.ErgoTree
import sigmastate.basics.DLogProtocol.ProveDlog

final case class OrderMeta(
  boxId: BoxId,
  boxValue: Long,
  script: ErgoTree,
  pk: ProveDlog,
  ts: Long
)
