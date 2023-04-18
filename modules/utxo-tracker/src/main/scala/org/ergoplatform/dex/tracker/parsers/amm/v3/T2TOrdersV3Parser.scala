package org.ergoplatform.dex.tracker.parsers.amm.v3

import cats.Functor
import cats.effect.Clock
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.amm.CFMMOrder.{DepositTokenFee, RedeemTokenFee, SwapTokenFee}
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.protocol.ErgoTreeSerializer
import org.ergoplatform.dex.protocol.amm.AMMType.T2T_CFMM
import org.ergoplatform.dex.protocol.amm.{ParserVersion, T2TCFMMTemplates}
import org.ergoplatform.dex.tracker.parsers.amm.CFMMOrdersParser
import org.ergoplatform.ergo.domain.Output
import org.ergoplatform.ergo.syntax._
import org.ergoplatform.ergo.{ErgoTreeTemplate, SErgoTree, TokenId}
import tofu.syntax.monadic._
import tofu.syntax.time.now.millis
import cats.syntax.option._

class T2TOrdersV3Parser[F[_]: Functor: Clock](spf: TokenId) extends CFMMOrdersParser[T2T_CFMM, ParserVersion.V3, F] {

  def deposit(box: Output): F[Option[CFMMOrder.AnyDeposit]] = millis.map { ts =>
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    if (template == T2TCFMMTemplates.depositV3) {
      for {
        poolId      <- tree.constants.parseBytea(13).map(PoolId.fromBytes)
        maxMinerFee <- tree.constants.parseLong(24)
        selfX       <- tree.constants.parseLong(8)
        selfY       <- tree.constants.parseLong(10)
        inX         <- box.assets.headOption.map(a => AssetAmount(a.tokenId, a.amount))
        inY         <- box.assets.lift(1).map(a => AssetAmount(a.tokenId, a.amount))
        dexFee <- if (inX.id == spf) (inX.value - selfX).some
                  else if (inY.id == spf) (inY.value - selfY).some
                  else box.assets.find(_.tokenId == spf).map(_.amount)
        redeemer <- tree.constants.parseBytea(14).map(SErgoTree.fromBytes)
        params = DepositParams(inX.withAmount(selfX), inY.withAmount(selfY), dexFee, redeemer)
      } yield DepositTokenFee(poolId, maxMinerFee, ts, params, box)
    } else None
  }

  def redeem(box: Output): F[Option[CFMMOrder.AnyRedeem]] = millis.map { ts =>
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    if (template == T2TCFMMTemplates.redeemV3) {
      for {
        poolId      <- tree.constants.parseBytea(13).map(PoolId.fromBytes)
        maxMinerFee <- tree.constants.parseLong(18)
        inLP        <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount))
        dexFee      <- box.assets.lift(1).map(a => AssetAmount(a.tokenId, a.amount))
        redeemer    <- tree.constants.parseBytea(14).map(SErgoTree.fromBytes)
        params = RedeemParams(inLP, dexFee.value, redeemer)
      } yield RedeemTokenFee(poolId, maxMinerFee, ts, params, box)
    } else None
  }

  def swap(box: Output): F[Option[CFMMOrder.AnySwap]] = millis.map { ts =>
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    if (template == T2TCFMMTemplates.swapV3) {
      for {
        poolId       <- tree.constants.parseBytea(18).map(PoolId.fromBytes)
        maxMinerFee  <- tree.constants.parseLong(33)
        maxExFee     <- tree.constants.parseLong(12)
        baseAmount   <- tree.constants.parseLong(3)
        inAmount     <- box.assets.headOption.map(a => AssetAmount(a.tokenId, baseAmount))
        outId        <- tree.constants.parseBytea(1).map(TokenId.fromBytes)
        minOutAmount <- tree.constants.parseLong(20)
        outAmount = AssetAmount(outId, minOutAmount)
        dexFeePerTokenDenom   <- tree.constants.parseLong(2)
        dexFeePerTokenNumDiff <- tree.constants.parseLong(13)
        dexFeePerTokenNum = dexFeePerTokenDenom - dexFeePerTokenNumDiff
        redeemer <- tree.constants.parseBytea(19).map(SErgoTree.fromBytes)
        params = SwapParams(
                   inAmount,
                   outAmount,
                   dexFeePerTokenNum,
                   dexFeePerTokenDenom,
                   redeemer
                 )
      } yield SwapTokenFee(poolId, maxMinerFee, ts, params, box, maxExFee)
    } else None
  }

}

object T2TOrdersV3Parser {

  def make[F[_]: Functor: Clock](spf: TokenId): CFMMOrdersParser[T2T_CFMM, ParserVersion.V3, F] =
    new T2TOrdersV3Parser(spf)
}
