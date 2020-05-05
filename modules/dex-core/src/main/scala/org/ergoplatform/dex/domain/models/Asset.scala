package org.ergoplatform.dex.domain.models

import io.circe.Decoder
import org.ergoplatform.dex.TokenId

/** A model mirroring Asset entity from Ergo node REST API.
  * See `Asset` in https://github.com/ergoplatform/ergo/blob/master/src/main/resources/api/openapi.yaml
  */
final case class Asset(
  tokenId: TokenId,
  amount: Long
)

object Asset {

  implicit val decoder: Decoder[Asset] = { cursor =>
    for {
      id  <- cursor.downField("tokenId").as[TokenId]
      amt <- cursor.downField("amount").as[Long]
    } yield Asset(id, amt)
  }
}
