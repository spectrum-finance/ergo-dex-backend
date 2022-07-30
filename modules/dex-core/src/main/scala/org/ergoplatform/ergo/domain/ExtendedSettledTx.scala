package org.ergoplatform.ergo.domain

import derevo.derive
import org.ergoplatform.ergo.{BlockId, TxId}
import org.ergoplatform.ergo.services.explorer.models.{Transaction => ExplorerTx}
import tofu.logging.derivation.loggable

@derive(loggable)
final case class ExtendedSettledTx(
  id: TxId,
  blockId: BlockId,
  inclusionHeight: Int,
  index: Int,
  globalIndex: Long,
  timestamp: Long,
  settledOutputs: List[SettledOutput],
  inputs: List[Input]
)

object ExtendedSettledTx {
  def fromExplorer(tx: ExplorerTx): ExtendedSettledTx =
    ExtendedSettledTx(
      tx.id,
      tx.blockId,
      tx.inclusionHeight,
      tx.index,
      tx.globalIndex,
      tx.timestamp,
      tx.outputs.map(SettledOutput.fromExplorer),
      tx.inputs.map(Input.fromExplorer)
    )
}