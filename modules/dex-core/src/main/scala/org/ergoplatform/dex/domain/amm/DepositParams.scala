package org.ergoplatform.dex.domain.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.dex.Address
import org.ergoplatform.dex.domain.AssetAmount
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
final case class DepositParams(
  poolId: PoolId,
  inX: AssetAmount,
  inY: AssetAmount,
  dexFee: Long,
  p2pk: Address
)
