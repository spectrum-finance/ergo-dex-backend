package org.ergoplatform.dex.domain

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.ergo.TokenId
import sttp.tapir.Schema
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
final case class FullAsset(
  id: TokenId,
  amount: Long,
  ticker: Option[Ticker],
  decimals: Option[Int]
) {
  def assetClass: AssetClass = AssetClass(id, ticker, decimals)
}

object FullAsset {
  implicit val schema: Schema[FullAsset] = Schema.derived
}
