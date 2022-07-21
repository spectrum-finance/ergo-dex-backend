package org.ergoplatform.dex.domain.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.ergo.{Address, PubKey}
import org.ergoplatform.dex.domain.AssetAmount
import scodec.Codec
import scodec.codecs.int64
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
final case class DepositParams(
  inX: AssetAmount,
  inY: AssetAmount,
  dexFee: Long,
  redeemer: PubKey
)

object DepositParams {

  implicit val codec: Codec[DepositParams] =
    (
      implicitly[Codec[AssetAmount]] ::
        implicitly[Codec[AssetAmount]] ::
        int64 ::
        implicitly[Codec[PubKey]]
    ).as[DepositParams]
}
