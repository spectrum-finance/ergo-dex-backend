package org.ergoplatform.dex.domain.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.ergo.Address
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
final case class SwapParams(
  input: AssetAmount,
  minOutput: AssetAmount,
  dexFeePerTokenNum: Long,
  dexFeePerTokenDenom: Long,
  p2pk: Address
)
