package org.ergoplatform.dex.domain.orderbook

import derevo.cats.show
import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.ergo.BoxId
import org.ergoplatform.dex.protocol.instances._
import org.ergoplatform.dex.protocol.codecs._
import sigmastate.Values.ErgoTree
import sigmastate.basics.DLogProtocol.ProveDlog
import tofu.logging.derivation.loggable

@derive(show, loggable, encoder, decoder)
final case class OrderMeta(
  boxId: BoxId,
  boxValue: Long,
  script: ErgoTree,
  pk: ProveDlog,
  ts: Long
)
