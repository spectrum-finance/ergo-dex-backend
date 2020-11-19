package org.ergoplatform.dex.protocol.models

import io.circe.{Decoder, Json}
import io.circe.refined._
import org.ergoplatform.dex.{BoxId, HexString}

/** A model mirroring ErgoTransactionOutput entity from Ergo node REST API.
  * See `ErgoTransactionOutput` in https://github.com/ergoplatform/ergo/blob/master/src/main/resources/api/openapi.yaml
  */
final case class Output(
  boxId: BoxId,
  value: Long,
  creationHeight: Int,
  ergoTree: HexString,
  assets: List[Asset],
  additionalRegisters: Json
)

object Output {

  implicit val decoder: Decoder[Output] = io.circe.derivation.deriveDecoder
}
