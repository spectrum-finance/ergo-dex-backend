package org.ergoplatform.dex.tracker.domain

import cats.data.NonEmptyList
import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.ErgoLikeTransaction
import org.ergoplatform.ergo.domain.Output
import org.ergoplatform.ergo.{BoxId, TxId}
import scorex.util.ModifierId
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
final case class Transaction(id: TxId, inputs: NonEmptyList[BoxId], outputs: NonEmptyList[Output])

object Transaction {

  def fromErgoLike(tx: ErgoLikeTransaction): Transaction =
    Transaction(
      TxId(ModifierId !@@ tx.id),
      NonEmptyList.fromListUnsafe(tx.inputs.map(_.boxId).map(BoxId.fromErgo).toList),
      NonEmptyList.fromListUnsafe(tx.outputs.toList.map(Output.fromErgoBox(_, tx.id))).sortBy(_.index)
    )
}
