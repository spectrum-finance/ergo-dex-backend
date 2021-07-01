package org.ergoplatform.dex.domain.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.dex.domain.amm.state.Predicted
import org.ergoplatform.dex.domain.{AssetAmount, BoxInfo}
import org.ergoplatform.dex.protocol.amm.constants
import sttp.tapir.{Schema, Validator}
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
final case class CFMMPool(
  poolId: PoolId,
  lp: AssetAmount,
  x: AssetAmount,
  y: AssetAmount,
  feeNum: Int,
  box: BoxInfo
) {

  def supplyLP: Long = constants.cfmm.TotalEmissionLP - lp.value

  def deposit(inX: AssetAmount, inY: AssetAmount, nextBox: BoxInfo): Predicted[CFMMPool] = {
    val unlocked = math.min(
      inX.value * supplyLP / x.value,
      inY.value * supplyLP / y.value
    )
    Predicted(copy(lp = lp - unlocked, x = x + inX, y = y + inY, box = nextBox))
  }

  def redeem(inLp: AssetAmount, nextBox: BoxInfo): Predicted[CFMMPool] = {
    val redeemedX = inLp.value * x.value / supplyLP
    val redeemedY = inLp.value * y.value / supplyLP
    Predicted(copy(lp = lp + inLp, x = x - redeemedX, y = y - redeemedY, box = nextBox))
  }

  def swap(in: AssetAmount, nextBox: BoxInfo): Predicted[CFMMPool] = {
    val (deltaX, deltaY) =
      if (in.id == x.id)
        (in.value, -y.value * in.value * feeNum / (x.value * constants.cfmm.FeeDenominator + in.value * feeNum))
      else
        (-x.value * in.value * feeNum / (y.value * constants.cfmm.FeeDenominator + in.value * feeNum), in.value)
    Predicted(copy(x = x + deltaX, y = y + deltaY, box = nextBox))
  }

  def rewardLP(inX: AssetAmount, inY: AssetAmount): AssetAmount =
    lp.withAmount(
      math.min(
        (BigInt(inX.value) * supplyLP / x.value).toLong,
        (BigInt(inY.value) * supplyLP / y.value).toLong
      )
    )

  def shares(lpIn: AssetAmount): (AssetAmount, AssetAmount) =
    x.withAmount(BigInt(lpIn.value) * x.value / supplyLP) ->
    y.withAmount(BigInt(lpIn.value) * y.value / supplyLP)

  def outputAmount(input: AssetAmount): AssetAmount = {
    def out(in: AssetAmount, out: AssetAmount) =
      out.withAmount(
        BigInt(out.value) * input.value * feeNum /
        (in.value * constants.cfmm.FeeDenominator + input.value * feeNum)
      )
    if (input.id == x.id) out(x, y) else out(y, x)
  }
}

object CFMMPool {
  implicit val schema: Schema[CFMMPool]       = Schema.derived[CFMMPool]
  implicit val validator: Validator[CFMMPool] = schema.validator
}
