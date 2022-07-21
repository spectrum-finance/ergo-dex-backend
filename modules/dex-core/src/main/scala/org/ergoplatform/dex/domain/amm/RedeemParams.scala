package org.ergoplatform.dex.domain.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.ergo.{Address, PubKey}
import org.ergoplatform.dex.domain.AssetAmount
import scodec.Codec
import scodec.codecs.int64
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
final case class RedeemParams(lp: AssetAmount, dexFee: Long, redeemer: PubKey)

object RedeemParams {

  implicit val codec: Codec[RedeemParams] =
    (implicitly[Codec[AssetAmount]] ::
      int64 ::
      implicitly[Codec[PubKey]]).as[RedeemParams]
}
