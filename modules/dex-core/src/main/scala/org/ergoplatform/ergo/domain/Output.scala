package org.ergoplatform.ergo.domain

import derevo.cats.show
import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.ErgoBox
import org.ergoplatform.ergo.services.explorer.models.{Output => ExplorerOutput}
import org.ergoplatform.ergo.services.node.models.{Output => NodeOutput}
import org.ergoplatform.ergo.{BoxId, SErgoTree, TokenId, TxId}
import scorex.crypto.authds.ADKey
import scorex.util.ModifierId
import scorex.util.encode.Base16
import tofu.logging.derivation.loggable

@derive(show, encoder, decoder, loggable)
final case class Output(
  boxId: BoxId,
  transactionId: TxId,
  value: Long,
  index: Int,
  creationHeight: Int,
  ergoTree: SErgoTree,
  assets: List[BoxAsset],
  additionalRegisters: Map[RegisterId, SConstant]
)

object Output {

  def fromExplorer(o: ExplorerOutput): Output =
    Output(
      o.boxId,
      o.transactionId,
      o.value,
      o.index,
      o.creationHeight,
      o.ergoTree,
      o.assets.map(BoxAsset.fromExplorer),
      o.additionalRegisters
    )

  def fromNode(o: NodeOutput): Output =
    Output(
      o.boxId,
      o.transactionId,
      o.value,
      o.index,
      o.creationHeight,
      o.ergoTree,
      o.assets.map(BoxAsset.fromNode),
      Map.empty // todo
    )

  def fromErgoBox(b: ErgoBox): Output = {
    val bId = ADKey !@@ b.id
    Output(
      BoxId(Base16.encode(bId)),
      TxId(ModifierId !@@ b.transactionId),
      b.value,
      b.index,
      b.creationHeight,
      SErgoTree.fromBytes(b.ergoTree.bytes),
      b.additionalTokens.toMap.map { case (id, l) => BoxAsset(TokenId.fromBytes(id), l) }.toList,
      Map.empty
    )
  }
}
