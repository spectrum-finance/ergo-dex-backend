package org.ergoplatform.dex.tracker.parsers.amm.v3

import cats.Functor
import cats.effect.Clock
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.amm.CFMMOrder.{Deposit, Redeem, SwapTokenFee}
import org.ergoplatform.dex.domain.amm.CFMMOrderType.FeeType
import org.ergoplatform.dex.domain.amm.CFMMOrderType.FeeType._
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.protocol.ErgoTreeSerializer
import org.ergoplatform.dex.protocol.amm.T2TCFMMTemplates
import org.ergoplatform.ergo.domain.Output
import org.ergoplatform.ergo.syntax._
import org.ergoplatform.ergo.{ErgoTreeTemplate, SErgoTree, TokenId}
import tofu.syntax.monadic._
import tofu.syntax.time.now.millis

class T2TOrderV3Parser[F[_]: Functor: Clock] {

  def deposit(box: Output): F[Option[Deposit[TokenFee, SErgoTree]]] = millis.map { ts =>
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    if (template == T2TCFMMTemplates.depositV3) {
      for {
        poolId      <- tree.constants.parseBytea(17).map(PoolId.fromBytes)
        maxMinerFee <- tree.constants.parseLong(27)
        dexFeeFromX <- tree.constants.parseBoolean(9)
        dexFeeFromY <- tree.constants.parseBoolean(13)
        inX         <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount))
        inY         <- box.assets.lift(1).map(a => AssetAmount(a.tokenId, a.amount))
        dexFee      <- tree.constants.parseLong(10)
        redeemer    <- tree.constants.parseBytea(0).map(SErgoTree.fromBytes)
        params = DepositParams(
                   if (dexFeeFromX) inX - dexFee else inX,
                   if (dexFeeFromY) inY - dexFee else inY,
                   dexFee,
                   redeemer
                 )
      } yield Deposit[TokenFee, SErgoTree](poolId, maxMinerFee, ts, params, box, FeeType.tokenFee)
    } else None
  }

  def redeem(box: Output): F[Option[Redeem[TokenFee, SErgoTree]]] = millis.map { ts =>
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    if (template == T2TCFMMTemplates.redeemV3) {
      for {
        poolId      <- tree.constants.parseBytea(13).map(PoolId.fromBytes)
        maxMinerFee <- tree.constants.parseLong(17)
        inLP        <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount))
        dexFee      <- box.assets.lift(1).map(a => AssetAmount(a.tokenId, a.amount))
        redeemer    <- tree.constants.parseBytea(0).map(SErgoTree.fromBytes)
        params = RedeemParams(inLP, dexFee.value, redeemer)
      } yield Redeem(poolId, maxMinerFee, ts, params, box, FeeType.tokenFee)
    } else None
  }

  def swap(box: Output): F[Option[SwapTokenFee]] = millis.map { ts =>
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    if (template == T2TCFMMTemplates.swapV3) {
      for {
        poolId       <- tree.constants.parseBytea(19).map(PoolId.fromBytes)
        maxMinerFee  <- tree.constants.parseLong(32)
        spectrumId   <- tree.constants.parseBytea(4).map(TokenId.fromBytes)
        maxExFee     <- tree.constants.parseLong(2)
        inAmount     <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount))
        outId        <- tree.constants.parseBytea(1).map(TokenId.fromBytes)
        minOutAmount <- tree.constants.parseLong(21)
        outAmount = AssetAmount(outId, minOutAmount)
        dexFeePerTokenNum   <- tree.constants.parseLong(24)
        dexFeePerTokenDenom <- tree.constants.parseLong(25)
        redeemer            <- tree.constants.parseBytea(20).map(SErgoTree.fromBytes)
        params = SwapParams(
                   if (spectrumId == inAmount.id) inAmount - maxExFee else inAmount,
                   outAmount,
                   dexFeePerTokenNum,
                   dexFeePerTokenDenom,
                   redeemer
                 )
      } yield SwapTokenFee(poolId, maxMinerFee, ts, params, box, maxExFee)
    } else None
  }

}
