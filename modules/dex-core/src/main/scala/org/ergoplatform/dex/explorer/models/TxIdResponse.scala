package org.ergoplatform.dex.explorer.models

import io.circe.Decoder
import io.circe.generic.semiauto._
import org.ergoplatform.dex.TxId

final case class TxIdResponse(id: TxId)

object TxIdResponse {

  implicit val decoder: Decoder[TxIdResponse] = deriveDecoder
}
