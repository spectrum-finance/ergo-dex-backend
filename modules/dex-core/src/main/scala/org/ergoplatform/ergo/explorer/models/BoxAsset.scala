package org.ergoplatform.ergo.explorer.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import doobie.util.Write
import org.ergoplatform.ergo.{TokenId, TokenType}
import shapeless.Lazy
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

object BoxAsset {
  implicit def write: Write[BoxAsset] = Lazy(implicitly[Write[BoxAsset]]).value
}
