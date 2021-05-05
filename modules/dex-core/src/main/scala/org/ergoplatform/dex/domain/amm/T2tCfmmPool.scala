package org.ergoplatform.dex.domain.amm

import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.network.Output

final case class T2tCfmmPool(
  poolId: PoolId,
  lp: AssetAmount,
  x: AssetAmount,
  y: AssetAmount,
  poolFee: Long,
  box: Output
) {

  def outputAmount(input: AssetAmount): AssetAmount =
    if (input.id == x.id)
      y.copy(amount = y.amount * input.amount * poolFee / (x.amount * 1000 + input.amount * poolFee))
    else
      x.copy(amount = x.amount * input.amount * poolFee / (y.amount * 1000 + input.amount * poolFee))
}
