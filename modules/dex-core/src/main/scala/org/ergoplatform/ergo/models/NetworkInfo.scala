package org.ergoplatform.ergo.models

import derevo.circe.decoder
import derevo.derive
import org.ergoplatform.ergo.BlockId
import tofu.logging.derivation.loggable

@derive(decoder, loggable)
final case class NetworkInfo(
  lastBlockId: BlockId,
  height: Int,
  maxBoxGix: Long,
  maxTxGix: Long,
  params: EpochParams
)
