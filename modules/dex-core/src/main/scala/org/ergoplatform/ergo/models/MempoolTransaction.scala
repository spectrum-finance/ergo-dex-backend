package org.ergoplatform.ergo.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.ergo.TxId
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
final case class MempoolTransaction(id: TxId, outputs: List[MempoolOutput])
