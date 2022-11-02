package org.ergoplatform.dex.domain.amm

import derevo.cats.show
import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.ergo.PubKey
import tofu.logging.derivation.loggable

@derive(show, encoder, decoder, loggable)
final case class DepositParams(
  inX: AssetAmount,
  inY: AssetAmount,
  dexFee: Long,
  redeemer: PubKey
)
