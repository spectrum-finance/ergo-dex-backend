package org.ergoplatform.ergo.models

import derevo.circe.decoder
import derevo.derive

@derive(decoder)
final case class EpochParams(
  id: Int,
  height: Int,
  storageFeeFactor: Int,
  minValuePerByte: Int,
  maxBlockSize: Int,
  maxBlockCost: Int,
  blockVersion: Byte,
  tokenAccessCost: Int,
  inputCost: Int,
  dataInputCost: Int,
  outputCost: Int
) {
  val safeMinValue: Long = (minValuePerByte * 500).toLong
}
