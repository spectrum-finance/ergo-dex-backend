package org.ergoplatform.dex.domain.network

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.dex.{TokenId, TokenType}
import tofu.logging.derivation.loggable

/** A model mirroring Asset entity from Ergo node REST API.
  * See `Asset` in https://github.com/ergoplatform/ergo/blob/master/src/main/resources/api/openapi.yaml
  */
@derive(encoder, decoder, loggable)
final case class BoxAsset(
  tokenId: TokenId,
  index: Int,
  amount: Long,
  name: Option[String],
  decimals: Option[Int],
  `type`: Option[TokenType]
)
