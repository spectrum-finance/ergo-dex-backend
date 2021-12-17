package org.ergoplatform.dex.domain

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.ergo.TokenId
import sttp.tapir.Schema

@derive(encoder, decoder)
final case class AssetClass(tokenId: TokenId, ticker: Option[Ticker], decimals: Option[Int]) {
  def assetInstance(amount: Long): FullAsset = FullAsset(tokenId, amount, ticker, decimals)
}

object AssetClass {
  implicit val schema: Schema[AssetClass] = Schema.derived
}
