package org.ergoplatform.dex.domain.locks

import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.ergo.Address

final case class LiquidityLock(deadline: Int, amount: AssetAmount, redeemer: Address)
