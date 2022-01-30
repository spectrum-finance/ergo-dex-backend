package org.ergoplatform.ergo.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.ergo.{Address, BoxId, SErgoTree}
import org.ergoplatform.ergo.models.ErgoBox._
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
final case class MinimalBox(
  boxId: BoxId,
  value: Long,
  ergoTree: SErgoTree,
  address: Address,
  assets: List[BoxAsset],
  additionalRegisters: Map[RegisterId, SConstant]
) extends ErgoBox
