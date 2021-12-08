package org.ergoplatform.dex.markets.models.amm

import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.amm.PoolId

final case class PoolSummary(id: PoolId, lockedX: AssetAmount, lockedY: AssetAmount)
