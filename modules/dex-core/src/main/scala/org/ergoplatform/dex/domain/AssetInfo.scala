package org.ergoplatform.dex.domain

import derevo.cats.show
import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.dex.protocol.constants.{NativeAssetDecimals, NativeAssetTicker}
import org.ergoplatform.ergo.models.BoxAsset
import scodec.Codec
import scodec.codecs.{bool, int32, optional}
import sttp.tapir.{Schema, Validator}
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable, show)
final case class AssetInfo(ticker: Option[Ticker], decimals: Option[Int])

object AssetInfo {

  def apply(ba: BoxAsset): AssetInfo = AssetInfo(ba.name.map(Ticker(_)), ba.decimals)

  val native: AssetInfo = AssetInfo(Some(NativeAssetTicker), Some(NativeAssetDecimals))

  implicit val codec: Codec[AssetInfo] =
    (optional(bool, implicitly[Codec[Ticker]]) :: optional(bool, int32)).as[AssetInfo]

  implicit val schema: Schema[AssetInfo]       = Schema.derived[AssetInfo]
  implicit val validator: Validator[AssetInfo] = schema.validator
}
