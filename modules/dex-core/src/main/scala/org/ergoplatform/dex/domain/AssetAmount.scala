package org.ergoplatform.dex.domain

import derevo.cats.show
import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.dex.TokenId
import org.ergoplatform.dex.domain.network.BoxAsset
import tofu.logging.derivation.loggable

@derive(show, encoder, decoder, loggable)
final case class AssetAmount(id: TokenId, amount: Long, ticker: Option[String]) {

  def >= (that: AssetAmount): Boolean = amount >= that.amount
}

object AssetAmount {

  def fromBoxAsset(boxAsset: BoxAsset): AssetAmount =
    AssetAmount(boxAsset.tokenId, boxAsset.amount, boxAsset.name)
}
