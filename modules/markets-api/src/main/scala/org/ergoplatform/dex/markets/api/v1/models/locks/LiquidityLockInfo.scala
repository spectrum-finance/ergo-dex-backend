package org.ergoplatform.dex.markets.api.v1.models.locks

import derevo.circe.encoder
import derevo.derive
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.dex.markets.db.models.locks.LiquidityLockStats
import org.ergoplatform.ergo.Address

@derive(encoder)
final case class LiquidityLockInfo(poolId: PoolId, deadline: Int, amount: Long, percent: BigDecimal, redeemer: Address)

object LiquidityLockInfo {

  def apply(lqs: LiquidityLockStats): LiquidityLockInfo =
    LiquidityLockInfo(lqs.poolId, lqs.deadline, lqs.amount, lqs.percent, lqs.redeemer)
}
