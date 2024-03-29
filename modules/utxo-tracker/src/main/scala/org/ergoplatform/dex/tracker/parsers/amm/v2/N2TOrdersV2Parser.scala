package org.ergoplatform.dex.tracker.parsers.amm.v2

import cats.effect.Clock
import cats.{Applicative, Monad}
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.amm.CFMMOrderType.SwapType
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.protocol.ErgoTreeSerializer
import org.ergoplatform.dex.protocol.amm.AMMType.N2T_CFMM
import org.ergoplatform.dex.protocol.amm.{ParserVersion, N2TCFMMTemplates => templates}
import org.ergoplatform.dex.tracker.parsers.amm.CFMMOrdersParser
import org.ergoplatform.ergo.domain.Output
import org.ergoplatform.ergo.syntax._
import org.ergoplatform.ergo.{ErgoTreeTemplate, SErgoTree, TokenId}
import sigmastate.Values.ErgoTree
import tofu.syntax.embed._
import tofu.syntax.foption.noneF
import tofu.syntax.monadic._
import tofu.syntax.time.now

final class N2TOrdersV2Parser[F[_]: Applicative: Clock](ts: Long)(implicit
  e: ErgoAddressEncoder
) extends CFMMOrdersParser[N2T_CFMM, ParserVersion.V2, F] {

  def deposit(box: Output): F[Option[CFMMOrder.AnyDeposit]] = noneF

  def redeem(box: Output): F[Option[CFMMOrder.AnyRedeem]] = noneF

  def swap(box: Output): F[Option[CFMMOrder.AnySwap]] = {
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    val parsed: Option[CFMMOrder.AnySwap] =
      if (template == templates.swapSellMultiAddressV2) swapSell(box, tree)
      else if (template == templates.swapBuyMultiAddressV2) swapBuy(box, tree)
      else None
    parsed.pure
  }

  private def swapSell(box: Output, tree: ErgoTree): Option[CFMMOrder.AnySwap] =
    for {
      poolId       <- tree.constants.parseBytea(8).map(PoolId.fromBytes)
      maxMinerFee  <- tree.constants.parseLong(23)
      baseAmount   <- tree.constants.parseLong(18).map(AssetAmount.native)
      outId        <- tree.constants.parseBytea(10).map(TokenId.fromBytes)
      minOutAmount <- tree.constants.parseLong(11)
      outAmount = AssetAmount(outId, minOutAmount)
      dexFeePerTokenNum   <- tree.constants.parseLong(12)
      dexFeePerTokenDenom <- tree.constants.parseLong(13)
      redeemer            <- tree.constants.parseBytea(9).map(SErgoTree.fromBytes)
      params = SwapParams(baseAmount, outAmount, dexFeePerTokenNum, dexFeePerTokenDenom, redeemer)
    } yield CFMMOrder.SwapMultiAddress(poolId, maxMinerFee, ts, params, box)

  private def swapBuy(box: Output, tree: ErgoTree): Option[CFMMOrder.AnySwap] =
    for {
      poolId       <- tree.constants.parseBytea(9).map(PoolId.fromBytes)
      maxMinerFee  <- tree.constants.parseLong(20)
      inAmount     <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount))
      minOutAmount <- tree.constants.parseLong(11)
      outAmount = AssetAmount.native(minOutAmount)
      dexFeePerTokenDenom   <- tree.constants.parseLong(5)
      dexFeePerTokenNumDiff <- tree.constants.parseLong(6)
      dexFeePerTokenNum = dexFeePerTokenDenom - dexFeePerTokenNumDiff
      redeemer <- tree.constants.parseBytea(10).map(SErgoTree.fromBytes)
      params = SwapParams(inAmount, outAmount, dexFeePerTokenNum, dexFeePerTokenDenom, redeemer)
    } yield CFMMOrder.SwapMultiAddress(poolId, maxMinerFee, ts, params, box)
}

object N2TOrdersV2Parser {

  def make[F[_]: Monad: Clock](implicit e: ErgoAddressEncoder): CFMMOrdersParser[N2T_CFMM, ParserVersion.V2, F] =
    now.millis
      .map(ts => new N2TOrdersV2Parser(ts): CFMMOrdersParser[N2T_CFMM, ParserVersion.V2, F])
      .embed
}
