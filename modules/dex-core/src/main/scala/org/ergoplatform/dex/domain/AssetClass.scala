package org.ergoplatform.dex.domain

import cats.Show
import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.ergo.TokenId
import sttp.tapir.Schema
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
final case class AssetClass(tokenId: TokenId, ticker: Option[Ticker], decimals: Option[Int]) {
  def assetInstance(amount: Long): FullAsset = FullAsset(tokenId, amount, ticker, decimals)
}

object AssetClass {

  implicit val show: Show[AssetClass] =
    Show.show(ac => s"Asset{id=${ac.tokenId}, ticker=${ac.ticker}, decimals=${ac.decimals}]")

  implicit val schema: Schema[AssetClass] = Schema.derived
}
