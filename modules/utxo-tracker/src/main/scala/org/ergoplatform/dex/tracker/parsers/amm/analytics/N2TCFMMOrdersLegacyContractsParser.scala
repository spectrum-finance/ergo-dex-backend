package org.ergoplatform.dex.tracker.parsers.amm.analytics

import cats.effect.Clock
import cats.{Applicative, Monad}
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.amm.CFMMVersionedOrder._
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.protocol.ErgoTreeSerializer
import org.ergoplatform.dex.protocol.amm.AMMType.N2T_CFMM
import org.ergoplatform.dex.protocol.amm.{N2TCFMMTemplates => templates}
import org.ergoplatform.ergo.domain.Output
import org.ergoplatform.ergo.syntax._
import org.ergoplatform.ergo.{ErgoTreeTemplate, PubKey, TokenId}
import sigmastate.Values.ErgoTree
import tofu.syntax.embed._
import tofu.syntax.monadic._
import tofu.syntax.time._

final class N2TCFMMOrdersLegacyContractsParser[F[_]: Applicative](ts: Long)(implicit
                                                                            e: ErgoAddressEncoder
) extends LegacyContractsParser[N2T_CFMM, F] {

  def depositV1(box: Output): F[Option[DepositV1]] = {
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    val parsed =
      if (template == templates.depositLegacyV1) {
        for {
          poolId   <- tree.constants.parseBytea(12).map(PoolId.fromBytes)
          inX      <- tree.constants.parseLong(16).map(AssetAmount.native)
          inY      <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount))
          dexFee   <- tree.constants.parseLong(15)
          redeemer <- tree.constants.parsePk(0).map(pk => PubKey.fromBytes(pk.pkBytes))
          params = DepositParams(inX, inY, dexFee, redeemer)
        } yield DepositV1(poolId, ts, params, box)
      } else None
    parsed.pure
  }

  def depositV0(box: Output): F[Option[DepositV0]] = {
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    val parsed =
      if (template == templates.depositLegacyV0) {
        for {
          poolId   <- tree.constants.parseBytea(9).map(PoolId.fromBytes)
          inX      <- tree.constants.parseLong(11).map(AssetAmount.native)
          inY      <- box.assets.lift(1).map(a => AssetAmount(a.tokenId, a.amount))
          dexFee   <- tree.constants.parseLong(11)
          redeemer <- tree.constants.parsePk(0).map(pk => PubKey.fromBytes(pk.pkBytes))
          params = DepositParams(inX, inY, dexFee, redeemer)
        } yield DepositV0(poolId, ts, params, box)
      } else None
    parsed.pure
  }

  def redeemV0(box: Output): F[Option[RedeemV0]] = {
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    val parsed =
      if (template == templates.redeemLegacyV0) {
        for {
          poolId   <- tree.constants.parseBytea(11).map(PoolId.fromBytes)
          inLP     <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount))
          dexFee   <- tree.constants.parseLong(12)
          redeemer <- tree.constants.parsePk(0).map(pk => PubKey.fromBytes(pk.pkBytes))
          params = RedeemParams(inLP, dexFee, redeemer)
        } yield RedeemV0(poolId, ts, params, box)
      } else None
    parsed.pure
  }

  def swapV0(box: Output): F[Option[SwapV0]] = {
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    val parsed =
      if (template == templates.swapSellLegacyV0) swapSellV0(box, tree)
      else if (template == templates.swapBuyLegacyV0) swapBuyV0(box, tree)
      else None
    parsed.pure
  }

  private def swapSellV0(box: Output, tree: ErgoTree): Option[SwapV0] =
    for {
      poolId       <- tree.constants.parseBytea(8).map(PoolId.fromBytes)
      baseAmount   <- tree.constants.parseLong(2).map(AssetAmount.native)
      outId        <- tree.constants.parseBytea(9).map(TokenId.fromBytes)
      minOutAmount <- tree.constants.parseLong(10)
      outAmount = AssetAmount(outId, minOutAmount)
      dexFeePerTokenNum   <- tree.constants.parseLong(11)
      dexFeePerTokenDenom <- tree.constants.parseLong(12)
      redeemer            <- tree.constants.parsePk(0).map(pk => PubKey.fromBytes(pk.pkBytes))
      params = SwapParams(baseAmount, outAmount, dexFeePerTokenNum, dexFeePerTokenDenom, redeemer)
    } yield SwapV0(poolId, ts, params, box)

  private def swapBuyV0(box: Output, tree: ErgoTree): Option[SwapV0] =
    for {
      poolId       <- tree.constants.parseBytea(9).map(PoolId.fromBytes)
      inAmount     <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount))
      minOutAmount <- tree.constants.parseLong(10)
      outAmount = AssetAmount.native(minOutAmount)
      dexFeePerTokenDenom   <- tree.constants.parseLong(5)
      dexFeePerTokenNumDiff <- tree.constants.parseLong(6)
      dexFeePerTokenNum = dexFeePerTokenDenom - dexFeePerTokenNumDiff
      redeemer <- tree.constants.parsePk(0).map(pk => PubKey.fromBytes(pk.pkBytes))
      params = SwapParams(inAmount, outAmount, dexFeePerTokenNum, dexFeePerTokenDenom, redeemer)
    } yield SwapV0(poolId, ts, params, box)
}

object N2TCFMMOrdersLegacyContractsParser {

  def make[F[_]: Monad: Clock](implicit e: ErgoAddressEncoder): LegacyContractsParser[N2T_CFMM, F] =
    now.millis.map(ts => new N2TCFMMOrdersLegacyContractsParser(ts): LegacyContractsParser[N2T_CFMM, F]).embed
}
