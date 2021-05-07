package org.ergoplatform.dex.domain.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.dex.Address
import org.ergoplatform.dex.domain.AssetAmount
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
final case class SwapParams(
  poolId: PoolId,
  input: AssetAmount,
  minOutput: AssetAmount,
  minerFee: Long,
  dexFeePerToken: Long,
  p2pk: Address
)
