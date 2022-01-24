package org.ergoplatform.dex.markets.db.models

import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.ergo.Address

object locks {

  final case class LiquidityLockStats(
    poolId: PoolId,
    deadline: Int,
    amount: Long,
    percent: BigDecimal,
    redeemer: Address
  )
}
