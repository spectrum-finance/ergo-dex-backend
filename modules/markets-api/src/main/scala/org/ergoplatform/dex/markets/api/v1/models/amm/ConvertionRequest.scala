package org.ergoplatform.dex.markets.api.v1.models.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.ergo.TokenId
import sttp.tapir.Schema

@derive(encoder, decoder)
case class ConvertionRequest(id: TokenId, amount: Long)

object ConvertionRequest {
  implicit val schemaConvertionReq: Schema[ConvertionRequest] = Schema.derived
}
