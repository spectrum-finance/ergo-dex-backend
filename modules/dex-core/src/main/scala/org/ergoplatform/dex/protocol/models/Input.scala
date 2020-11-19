package org.ergoplatform.dex.protocol.models

import io.circe.Decoder
import org.ergoplatform.dex.BoxId

/** A model mirroring ErgoTransactionInput entity from Ergo node REST API.
  * See `ErgoTransactionInput` in https://github.com/ergoplatform/ergo/blob/master/src/main/resources/api/openapi.yaml
  */
final case class Input(boxId: BoxId)

object Input {

  implicit val decoder: Decoder[Input] = io.circe.derivation.deriveDecoder
}
