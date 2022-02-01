package org.ergoplatform.ergo.domain

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.common.HexString
import org.ergoplatform.ergo._
import tofu.logging.Loggable
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
final case class Input(
  boxId: BoxId,
  value: Long,
  index: Int,
  spendingProof: Option[HexString],
  outputTransactionId: TxId,
  outputIndex: Int,
  outputGlobalIndex: Long,
  outputCreatedAt: Int,
  outputSettledAt: Int,
  ergoTree: SErgoTree,
  address: Address,
  assets: List[BoxAsset],
  additionalRegisters: Map[RegisterId, SConstant]
) {

  def asOutput: Output = Output(
    boxId,
    outputTransactionId,
    value,
    outputIndex,
    outputCreatedAt,
    ergoTree,
    address,
    assets,
    additionalRegisters
  )
}
