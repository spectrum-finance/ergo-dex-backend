package org.ergoplatform.dex.domain.amm

import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.network.Output
import org.ergoplatform.dex.protocol.amm.constants

final case class CfmmPool(
  poolId: PoolId,
  lp: AssetAmount,
  x: AssetAmount,
  y: AssetAmount,
  poolFee: Long,
  box: Output
) {

  def supplyLP: Long = constants.cfmm.TotalEmissionLP - lp.value

  def rewardLP(inX: AssetAmount, inY: AssetAmount): AssetAmount =
    lp.withAmount(
      math.min(
        (BigInt(inX.value) * supplyLP / x.value).toLong,
        (BigInt(inY.value) * supplyLP / y.value).toLong
      )
    )

  def shares(lp: AssetAmount): (AssetAmount, AssetAmount) =
    x.withAmount(BigInt(lp.value) * x.value / supplyLP) ->
    y.withAmount(BigInt(lp.value) * y.value / supplyLP)

  def outputAmount(input: AssetAmount): AssetAmount = {
    def out(in: AssetAmount, out: AssetAmount) =
      out.withAmount(
        BigInt(out.value) * input.value * poolFee /
        (in.value * constants.cfmm.FeeDenominator + input.value * poolFee)
      )
    if (input.id == x.id) out(x, y) else out(y, x)
  }
}
