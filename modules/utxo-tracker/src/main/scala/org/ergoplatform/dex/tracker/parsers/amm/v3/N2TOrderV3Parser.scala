package org.ergoplatform.dex.tracker.parsers.amm.v3

import cats.Functor
import cats.effect.Clock
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.protocol.ErgoTreeSerializer
import org.ergoplatform.dex.protocol.amm.N2TCFMMTemplates
import org.ergoplatform.ergo.domain.Output
import org.ergoplatform.ergo.syntax._
import org.ergoplatform.ergo.{ErgoTreeTemplate, PubKey, SErgoTree, TokenId}
import sigmastate.Values.ErgoTree
import tofu.syntax.monadic._
import tofu.syntax.time.now.millis

class N2TOrderV3Parser[F[_]: Functor: Clock] {

  def deposit(box: Output): F[Option[CFMMOrder.DepositTokenFee]] = millis.map { ts =>
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    if (template == N2TCFMMTemplates.depositV3) {
      for {
        poolId      <- tree.constants.parseBytea(14).map(PoolId.fromBytes)
        maxMinerFee <- tree.constants.parseLong(22)
        depositFee  <- tree.constants.parseBoolean(10).map(if (_) DepositFee.FeeFromY else DepositFee.FeeFromInput)
        inX         <- tree.constants.parseLong(2).map(AssetAmount.native)
        inY         <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount))
        dexFee      <- tree.constants.parseLong(11)
        redeemer    <- tree.constants.parsePk(0).map(pk => PubKey.fromBytes(pk.pkBytes))
        params = DepositParams(inX, inY, dexFee, redeemer)
      } yield CFMMOrder.DepositTokenFee(poolId, maxMinerFee, ts, params, box, depositFee)
    } else None
  }

  def redeem(box: Output): F[Option[CFMMOrder.RedeemTokenFee]] = millis.map { ts =>
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    if (template == N2TCFMMTemplates.redeemV3) {
      for {
        poolId      <- tree.constants.parseBytea(11).map(PoolId.fromBytes)
        maxMinerFee <- tree.constants.parseLong(15)
        inLP        <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount))
        dexFee      <- tree.constants.parseLong(12)
        redeemer    <- tree.constants.parsePk(0).map(pk => PubKey.fromBytes(pk.pkBytes))
        params = RedeemParams(inLP, dexFee, redeemer)
      } yield CFMMOrder.RedeemTokenFee(poolId, maxMinerFee, ts, params, box)
    } else None
  }

  def swap(box: Output): F[Option[CFMMOrder.SwapAny]] = millis.map { ts =>
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    if (template == N2TCFMMTemplates.swapSellV3) swapSell(box, tree, ts)
    else if (template == N2TCFMMTemplates.swapBuyV3) swapBuy(box, tree, ts)
    else None
  }

  private def swapSell(box: Output, tree: ErgoTree, ts: Long): Option[CFMMOrder.SwapTokenFee] =
    for {
      poolId       <- tree.constants.parseBytea(11).map(PoolId.fromBytes)
      maxMinerFee  <- tree.constants.parseLong(32)
      baseAmount   <- tree.constants.parseLong(24).map(AssetAmount.native)
      outId        <- tree.constants.parseBytea(13).map(TokenId.fromBytes)
      minOutAmount <- tree.constants.parseLong(14)
      outAmount = AssetAmount(outId, minOutAmount)
      dexFeePerTokenNum   <- tree.constants.parseLong(17)
      dexFeePerTokenDenom <- tree.constants.parseLong(18)
      redeemer            <- tree.constants.parseBytea(12).map(SErgoTree.fromBytes)
      params = SwapParams(baseAmount, outAmount, dexFeePerTokenNum, dexFeePerTokenDenom, redeemer)
      reserveExFee <- tree.constants.parseLong(1)
    } yield CFMMOrder.SwapTokenFee(poolId, maxMinerFee, ts, params, box, reserveExFee)

  private def swapBuy(box: Output, tree: ErgoTree, ts: Long): Option[CFMMOrder.SwapTokenFee] =
    for {
      poolId         <- tree.constants.parseBytea(12).map(PoolId.fromBytes)
      maxMinerFee    <- tree.constants.parseLong(24)
      spectrumId     <- tree.constants.parseBytea(1).map(TokenId.fromBytes)
      inAmount       <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount))
      minQuoteAmount <- tree.constants.parseLong(14)
      outAmount = AssetAmount.native(minQuoteAmount)
      dexFeePerTokenDenom   <- tree.constants.parseLong(10) //todo ?
      dexFeePerTokenNumDiff <- tree.constants.parseLong(9)
      dexFeePerTokenNum = dexFeePerTokenDenom - dexFeePerTokenNumDiff
      reserveExFee <- tree.constants.parseLong(8)
      redeemer     <- tree.constants.parseBytea(14).map(SErgoTree.fromBytes)
      baseAmount = if (spectrumId == inAmount.id) inAmount - reserveExFee else inAmount
      params     = SwapParams(baseAmount, outAmount, dexFeePerTokenNum, dexFeePerTokenDenom, redeemer)
    } yield CFMMOrder.SwapTokenFee(poolId, maxMinerFee, ts, params, box, reserveExFee)
}
