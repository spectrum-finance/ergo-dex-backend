package org.ergoplatform.dex.domain.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.ergo.{Address, PubKey}
import scodec.Codec
import scodec.codecs.int64
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
final case class SwapParams(
  input: AssetAmount,
  minOutput: AssetAmount,
  dexFeePerTokenNum: Long,
  dexFeePerTokenDenom: Long,
  redeemer: PubKey
)

object SwapParams {

  implicit val codec: Codec[SwapParams] =
    (implicitly[Codec[AssetAmount]] ::
      implicitly[Codec[AssetAmount]] ::
      int64 ::
      int64 ::
      implicitly[Codec[PubKey]]).as[SwapParams]
}
