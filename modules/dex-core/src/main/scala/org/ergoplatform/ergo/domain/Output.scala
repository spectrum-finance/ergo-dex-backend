package org.ergoplatform.ergo.domain

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.ergo.services.explorer.models.{Output => ExplorerOutput}
import org.ergoplatform.ergo.services.node.models.{Output => NodeOutput}
import org.ergoplatform.ergo.{BoxId, SErgoTree, TxId}
import scodec.Codec
import scodec.codecs.{int32, int64, list}
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
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

  private val tupleCodec: Codec[(RegisterId, SConstant)] =
    implicitly[Codec[RegisterId]].pairedWith(implicitly[Codec[SConstant]])

  private val mapCodec: Codec[Map[RegisterId, SConstant]] =
    list(tupleCodec).xmap(_.toMap, _.toList)

  implicit val codec: Codec[Output] =
    (
      implicitly[Codec[BoxId]] ::
        implicitly[Codec[TxId]] ::
        int64 ::
        int32 ::
        int32 ::
        implicitly[Codec[SErgoTree]] ::
        list(implicitly[Codec[BoxAsset]]) ::
        mapCodec
    ).as[Output]
}
