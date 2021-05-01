package org.ergoplatform.dex.domain.network

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.dex.{Address, BoxId, SErgoTree, TxId}
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
final case class Output(
  boxId: BoxId,
  transactionId: TxId,
  value: Long,
  index: Int,
  creationHeight: Int,
  settlementHeight: Int,
  ergoTree: SErgoTree,
  address: Address,
  assets: List[Asset]
) extends ErgoBox
