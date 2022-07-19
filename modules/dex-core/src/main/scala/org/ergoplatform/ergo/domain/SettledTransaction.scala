package org.ergoplatform.ergo.domain

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.ergo.BlockId
import tofu.logging.derivation.loggable
import org.ergoplatform.ergo.services.explorer.models.{Transaction => ExplorerTx}

@derive(encoder, decoder, loggable)
final case class SettledTransaction(
  tx: Transaction,
  blockId: BlockId,
  inclusionHeight: Int,
  index: Int,
  globalIndex: Long,
  timestamp: Long
)

object SettledTransaction {

  def fromExplorer(tx: ExplorerTx): SettledTransaction =
    SettledTransaction(
      Transaction.fromExplorer(tx),
      tx.blockId,
      tx.inclusionHeight,
      tx.index,
      tx.globalIndex,
      tx.timestamp
    )

  def fromExtendedSettledTx(tx: ExtendedSettledTx): SettledTransaction =
    SettledTransaction(
      Transaction(tx.id, tx.inputs, tx.settledOutputs.map(_.output)),
      tx.blockId,
      tx.inclusionHeight,
      tx.index,
      tx.globalIndex,
      tx.timestamp
    )
}
