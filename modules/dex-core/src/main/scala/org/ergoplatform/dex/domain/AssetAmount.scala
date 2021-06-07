package org.ergoplatform.dex.domain

import derevo.cats.show
import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.dex.TokenId
import org.ergoplatform.network.BoxAsset
import tofu.logging.derivation.loggable

@derive(show, encoder, decoder, loggable)
final case class AssetAmount(id: TokenId, value: Long, ticker: Option[String]) {

  def >=(that: AssetAmount): Boolean = value >= that.value

  def withAmount(x: Long): AssetAmount = copy(value = x)

  def withAmount(x: BigInt): AssetAmount = copy(value = x.toLong)
}

object AssetAmount {

  def fromBoxAsset(boxAsset: BoxAsset): AssetAmount =
    AssetAmount(boxAsset.tokenId, boxAsset.amount, boxAsset.name)
}
