package org.ergoplatform.dex.tracker.parsers.amm

import cats.effect.Clock
import cats.{Applicative, Monad}
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.amm.CFMMOrder.{Deposit, Redeem, SwapErgAny}
import org.ergoplatform.dex.domain.amm.CFMMOrderType.{FeeType, SwapType}
import org.ergoplatform.dex.domain.amm.CFMMOrderType.FeeType.ErgFee
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.protocol.ErgoTreeSerializer
import org.ergoplatform.dex.protocol.amm.AMMType.N2T_CFMM
import org.ergoplatform.dex.protocol.amm.{ParserType, N2TCFMMTemplates => templates}
import org.ergoplatform.ergo.domain.Output
import org.ergoplatform.ergo.syntax._
import org.ergoplatform.ergo.{ErgoTreeTemplate, PubKey, TokenId}
import sigmastate.Values.ErgoTree
import tofu.syntax.embed._
import tofu.syntax.monadic._
import tofu.syntax.time._

final class N2TCFMMOrdersParserP2Pk[F[_]: Applicative](ts: Long)(implicit
  e: ErgoAddressEncoder
) extends CFMMOrdersParser[N2T_CFMM, ParserType.Default, F] {

  def deposit(box: Output): F[Option[Deposit[ErgFee, PubKey]]] = {
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    val parsed: Option[Deposit[ErgFee, PubKey]] =
      if (template == templates.depositV1) {
        for {
          poolId      <- tree.constants.parseBytea(12).map(PoolId.fromBytes)
          maxMinerFee <- tree.constants.parseLong(22)
          inX         <- tree.constants.parseLong(16).map(AssetAmount.native)
          inY         <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount))
          dexFee      <- tree.constants.parseLong(15)
          redeemer    <- tree.constants.parsePk(0).map(pk => PubKey.fromBytes(pk.pkBytes))
          params = DepositParams(inX, inY, dexFee, redeemer)
        } yield CFMMOrder.Deposit(poolId, maxMinerFee, ts, params, box, FeeType.ergFee)
      } else None
    parsed.pure
  }

  def redeem(box: Output): F[Option[Redeem[ErgFee, PubKey]]] = {
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    val parsed: Option[Redeem[ErgFee, PubKey]] =
      if (template == templates.redeemV1) {
        for {
          poolId      <- tree.constants.parseBytea(11).map(PoolId.fromBytes)
          maxMinerFee <- tree.constants.parseLong(16)
          inLP        <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount))
          dexFee      <- tree.constants.parseLong(12)
          redeemer    <- tree.constants.parsePk(0).map(pk => PubKey.fromBytes(pk.pkBytes))
          params = RedeemParams(inLP, dexFee, redeemer)
        } yield CFMMOrder.Redeem(poolId, maxMinerFee, ts, params, box, FeeType.ergFee)
      } else None
    parsed.pure
  }

  def swap(box: Output): F[Option[SwapErgAny]] = {
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    val parsed: Option[SwapErgAny] =
      if (template == templates.swapSellV1) swapSell(box, tree)
      else if (template == templates.swapBuyV1) swapBuy(box, tree)
      else None
    parsed.pure
  }

  private def swapSell(box: Output, tree: ErgoTree): Option[SwapErgAny] =
    for {
      poolId       <- tree.constants.parseBytea(8).map(PoolId.fromBytes)
      maxMinerFee  <- tree.constants.parseLong(22)
      baseAmount   <- tree.constants.parseLong(2).map(AssetAmount.native)
      outId        <- tree.constants.parseBytea(9).map(TokenId.fromBytes)
      minOutAmount <- tree.constants.parseLong(10)
      outAmount = AssetAmount(outId, minOutAmount)
      dexFeePerTokenNum   <- tree.constants.parseLong(11)
      dexFeePerTokenDenom <- tree.constants.parseLong(12)
      redeemer            <- tree.constants.parsePk(0).map(pk => PubKey.fromBytes(pk.pkBytes))
      params = SwapParams(baseAmount, outAmount, dexFeePerTokenNum, dexFeePerTokenDenom, redeemer)
    } yield CFMMOrder.Swap(poolId, maxMinerFee, ts, params, box, SwapType.swapP2Pk)

  private def swapBuy(box: Output, tree: ErgoTree): Option[SwapErgAny] =
    for {
      poolId       <- tree.constants.parseBytea(9).map(PoolId.fromBytes)
      maxMinerFee  <- tree.constants.parseLong(19)
      inAmount     <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount))
      minOutAmount <- tree.constants.parseLong(10)
      outAmount = AssetAmount.native(minOutAmount)
      dexFeePerTokenDenom   <- tree.constants.parseLong(5)
      dexFeePerTokenNumDiff <- tree.constants.parseLong(6)
      dexFeePerTokenNum = dexFeePerTokenDenom - dexFeePerTokenNumDiff
      redeemer <- tree.constants.parsePk(0).map(pk => PubKey.fromBytes(pk.pkBytes))
      params = SwapParams(inAmount, outAmount, dexFeePerTokenNum, dexFeePerTokenDenom, redeemer)
    } yield CFMMOrder.Swap(poolId, maxMinerFee, ts, params, box, SwapType.swapP2Pk)
}

object N2TCFMMOrdersParserP2Pk {

  def make[F[_]: Monad: Clock](implicit e: ErgoAddressEncoder): CFMMOrdersParser[N2T_CFMM, ParserType.Default, F] =
    now.millis.map(ts => new N2TCFMMOrdersParserP2Pk(ts): CFMMOrdersParser[N2T_CFMM, ParserType.Default, F]).embed
}
