package org.ergoplatform.dex.domain.amm

import derevo.cats.show
import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.dex.domain.AssetAmount
import tofu.logging.derivation.loggable

@derive(show, encoder, decoder, loggable)
final case class SwapParams[T](
  input: AssetAmount,
  minOutput: AssetAmount,
  dexFeePerTokenNum: Long,
  dexFeePerTokenDenom: Long,
  redeemer: T
)
