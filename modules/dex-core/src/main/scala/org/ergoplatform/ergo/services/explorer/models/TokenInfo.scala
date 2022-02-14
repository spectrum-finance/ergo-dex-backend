package org.ergoplatform.ergo.services.explorer.models

import derevo.circe.decoder
import derevo.derive
import org.ergoplatform.dex.protocol.constants._
import org.ergoplatform.ergo.{BoxId, TokenId}
import tofu.logging.derivation.loggable

@derive(decoder, loggable)
final case class TokenInfo(
  id: TokenId,
  boxId: BoxId,
  emissionAmount: Long,
  name: Option[String],
  description: Option[String],
  decimals: Option[Int]
)

object TokenInfo {

  val ErgoTokenInfo: TokenInfo =
    TokenInfo(
      ErgoAssetId,
      ErgoGenesisBox,
      ErgoEmissionAmount,
      Some(ErgoAssetTicker.value),
      Some(ErgoAssetDescription),
      Some(ErgoAssetDecimals)
    )
}
