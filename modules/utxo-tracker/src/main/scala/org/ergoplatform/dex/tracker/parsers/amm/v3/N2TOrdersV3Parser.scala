package org.ergoplatform.dex.tracker.parsers.amm.v3

import cats.Functor
import cats.effect.Clock
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.amm.CFMMOrder.{DepositTokenFee, RedeemTokenFee}
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.protocol.ErgoTreeSerializer
import org.ergoplatform.dex.protocol.amm.AMMType.N2T_CFMM
import org.ergoplatform.dex.protocol.amm.{N2TCFMMTemplates, ParserVersion}
import org.ergoplatform.dex.tracker.parsers.amm.CFMMOrdersParser
import org.ergoplatform.ergo.domain.Output
import org.ergoplatform.ergo.syntax._
import org.ergoplatform.ergo.{ErgoTreeTemplate, SErgoTree, TokenId}
import sigmastate.Values.ErgoTree
import tofu.syntax.monadic._
import tofu.syntax.time.now.millis
import cats.syntax.option._

final class N2TOrdersV3Parser[F[_]: Functor: Clock](spf: TokenId)
  extends CFMMOrdersParser[N2T_CFMM, ParserVersion.V3, F] {

  def deposit(box: Output): F[Option[CFMMOrder.AnyDeposit]] = millis.map { ts =>
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    if (template == N2TCFMMTemplates.depositV3) {
      for {
        poolId         <- tree.constants.parseBytea(12).map(PoolId.fromBytes)
        maxMinerFee    <- tree.constants.parseLong(21)
        selfX          <- tree.constants.parseLong(1).map(AssetAmount.native)
        selfYAmount    <- tree.constants.parseLong(9)
        selfYBoxAmount <- box.assets.headOption.map(a => AssetAmount(a.tokenId, a.amount))
        selfY = selfYBoxAmount.withAmount(selfYAmount)
        dexFee <- if (selfY.id == spf) (selfYBoxAmount.value - selfY.value).some
                  else box.assets.find(_.tokenId == spf).map(_.amount)
        redeemer <- tree.constants.parseBytea(13).map(SErgoTree.fromBytes)
        params = DepositParams(selfX, selfY, dexFee, redeemer)
      } yield DepositTokenFee(poolId, maxMinerFee, ts, params, box)
    } else None
  }

  def redeem(box: Output): F[Option[CFMMOrder.AnyRedeem]] = millis.map { ts =>
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    if (template == N2TCFMMTemplates.redeemV3) {
      for {
        poolId      <- tree.constants.parseBytea(11).map(PoolId.fromBytes)
        maxMinerFee <- tree.constants.parseLong(16)
        inLP        <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount))
        dexFee      <- box.assets.lift(1).map(a => AssetAmount(a.tokenId, a.amount))
        redeemer    <- tree.constants.parseBytea(12).map(SErgoTree.fromBytes)
        params = RedeemParams(inLP, dexFee.value, redeemer)
      } yield RedeemTokenFee(poolId, maxMinerFee, ts, params, box)
    } else None
  }

  def swap(box: Output): F[Option[CFMMOrder.AnySwap]] = millis.map { ts =>
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    if (template == N2TCFMMTemplates.swapSellV3) swapSell(box, tree, ts)
    else if (template == N2TCFMMTemplates.swapBuyV3) swapBuy(box, tree, ts)
    else None
  }

  private def swapSell(box: Output, tree: ErgoTree, ts: Long): Option[CFMMOrder.AnySwap] =
    for {
      baseAmount   <- tree.constants.parseLong(3).map(AssetAmount.native)
      poolId       <- tree.constants.parseBytea(13).map(PoolId.fromBytes)
      maxMinerFee  <- tree.constants.parseLong(31)
      outId        <- tree.constants.parseBytea(15).map(TokenId.fromBytes)
      minOutAmount <- tree.constants.parseLong(16)
      outAmount = AssetAmount(outId, minOutAmount)
      dexFeePerTokenDenom   <- tree.constants.parseLong(1)
      dexFeePerTokenNumDiff <- tree.constants.parseLong(2)
      dexFeePerTokenNum = dexFeePerTokenDenom - dexFeePerTokenNumDiff
      redeemer <- tree.constants.parseBytea(14).map(SErgoTree.fromBytes)
      params = SwapParams(baseAmount, outAmount, dexFeePerTokenNum, dexFeePerTokenDenom, redeemer)
      reserveExFee <- tree.constants.parseLong(11)
    } yield CFMMOrder.SwapTokenFee(poolId, maxMinerFee, ts, params, box, reserveExFee)

  private def swapBuy(box: Output, tree: ErgoTree, ts: Long): Option[CFMMOrder.AnySwap] =
    for {
      poolId         <- tree.constants.parseBytea(11).map(PoolId.fromBytes)
      maxMinerFee    <- tree.constants.parseLong(24)
      inAmount       <- tree.constants.parseLong(1)
      inAssetAmount  <- box.assets.headOption.map(a => AssetAmount(a.tokenId, a.amount)).map(_.withAmount(inAmount))
      minQuoteAmount <- tree.constants.parseLong(13)
      outAmount = AssetAmount.native(minQuoteAmount)
      dexFeePerTokenDenom <- tree.constants.parseLong(8)
      dexFeePerTokenNum   <- tree.constants.parseLong(9)
      reserveExFee        <- tree.constants.parseLong(7)
      redeemer            <- tree.constants.parseBytea(12).map(SErgoTree.fromBytes)
      baseAmount = inAssetAmount
      params     = SwapParams(baseAmount, outAmount, dexFeePerTokenNum, dexFeePerTokenDenom, redeemer)
    } yield CFMMOrder.SwapTokenFee(poolId, maxMinerFee, ts, params, box, reserveExFee)
}

object N2TOrdersV3Parser {

  def make[F[_]: Functor: Clock](spf: TokenId): CFMMOrdersParser[N2T_CFMM, ParserVersion.V3, F] =
    new N2TOrdersV3Parser(spf)
}
