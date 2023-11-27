package org.ergoplatform.ergo.domain

import derevo.cats.show
import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.ErgoBox
import org.ergoplatform.ErgoBox.NonMandatoryRegisterId
import org.ergoplatform.dex.domain.DexOperatorOutput
import org.ergoplatform.dex.protocol.ErgoTreeSerializer.default._
import org.ergoplatform.ergo.domain.sigma.renderEvaluatedValue
import org.ergoplatform.ergo.services.explorer.models.{Output => ExplorerOutput}
import org.ergoplatform.ergo.services.node.models.{Output => NodeOutput}
import org.ergoplatform.ergo.state.{Predicted, Traced}
import org.ergoplatform.ergo.{BoxId, SErgoTree, TxId}
import scorex.util.ModifierId
import sigmastate.SType
import sigmastate.Values.EvaluatedValue
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

  def predicted(output: Output, prevBoxId: BoxId): Traced[Predicted[DexOperatorOutput]] =
    Traced(Predicted(DexOperatorOutput(output)), prevBoxId)

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

  def fromErgoBox(box: ErgoBox, txId: ModifierId): Output =
    Output(
      BoxId.fromErgo(box.id),
      TxId(ModifierId !@@ txId),
      box.value,
      box.index,
      box.creationHeight,
      serialize(box.ergoTree),
      box.additionalTokens.toArray.toList.map { case (id, amount) => BoxAsset.fromErgo(id, amount) },
      parseRegisters(box.additionalRegisters)
    )

  private def parseRegisters(
    additionalRegisters: Map[NonMandatoryRegisterId, _ <: EvaluatedValue[_ <: SType]]
  ): Map[RegisterId, SConstant] =
    additionalRegisters.flatMap { case (k, v) =>
      for {
        register  <- RegisterId.withNameOption(s"R${k.number}")
        sConstant <- renderEvaluatedValue(v).map { case (t, eval) => SConstant.fromRenderValue(t, eval) }
      } yield (register, sConstant)
    }
}
