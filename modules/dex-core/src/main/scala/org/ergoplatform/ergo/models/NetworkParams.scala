package org.ergoplatform.ergo.models

import derevo.circe.decoder
import derevo.derive

@derive(decoder)
final case class NetworkParams(minValuePerByte: Long) {
  val safeMinValue: Long = minValuePerByte * 500
}
