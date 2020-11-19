package org.ergoplatform.dex.protocol.models

import io.circe.Decoder
import org.ergoplatform.dex.AssetId

/** A model mirroring Asset entity from Ergo node REST API.
  * See `Asset` in https://github.com/ergoplatform/ergo/blob/master/src/main/resources/api/openapi.yaml
  */
final case class Asset(
  tokenId: AssetId,
  amount: Long
)

object Asset {

  implicit val decoder: Decoder[Asset] = io.circe.derivation.deriveDecoder
}
