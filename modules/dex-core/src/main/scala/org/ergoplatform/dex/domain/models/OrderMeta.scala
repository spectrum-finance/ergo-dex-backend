package org.ergoplatform.dex.domain.models

import derevo.cats.show
import derevo.derive
import org.ergoplatform.dex.BoxId
import org.ergoplatform.dex.protocol.instances._
import sigmastate.Values.ErgoTree
import sigmastate.basics.DLogProtocol.ProveDlog
import tofu.logging.derivation.loggable

@derive(show, loggable)
final case class OrderMeta(
  boxId: BoxId,
  boxValue: Long,
  script: ErgoTree,
  pk: ProveDlog,
  ts: Long
)
