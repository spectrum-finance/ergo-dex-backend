package org.ergoplatform.ergo.domain

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.common.HexString
import org.ergoplatform.ergo._
import org.ergoplatform.ergo.services.explorer.models.{Input => ExplorerIn}
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
final case class Input(
  boxId: BoxId,
  spendingProof: Option[HexString],
  output: Output
)

object Input {

  def fromExplorer(in: ExplorerIn): Input =
    Input(
      in.boxId,
      in.spendingProof,
      Output(
        in.boxId,
        in.outputTransactionId,
        in.value,
        in.outputIndex,
        in.outputCreatedAt,
        in.ergoTree,
        in.assets.map(BoxAsset.fromExplorer),
        in.additionalRegisters
      )
    )
}
