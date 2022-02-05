package org.ergoplatform.ergo.domain

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.ergo.services.explorer.models.{Transaction => ExplorerTx}
import org.ergoplatform.ergo.TxId
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
final case class Transaction(
  id: TxId,
  inputs: List[Input],
  outputs: List[Output]
)

object Transaction {

  def fromExplorer(tx: ExplorerTx): Transaction =
    Transaction(tx.id, tx.inputs.map(Input.fromExplorer), tx.outputs.map(Output.fromExplorer))
}
