package org.ergoplatform.dex.tracker.parsers.amm

import cats.effect.Clock
import cats.{Applicative, Monad}
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.protocol.ErgoTreeSerializer
import org.ergoplatform.dex.protocol.amm.AMMType.T2T_CFMM
import org.ergoplatform.dex.protocol.amm.{ParserType, T2TCFMMTemplates => templates}
import org.ergoplatform.ergo.domain.Output
import org.ergoplatform.ergo.syntax._
import org.ergoplatform.ergo.{ErgoTreeTemplate, PubKey, TokenId}
import tofu.syntax.embed._
import tofu.syntax.monadic._
import tofu.syntax.time.now

final class T2TCFMMOrdersParserP2Pk[F[_]: Applicative: Clock](ts: Long)(implicit
  e: ErgoAddressEncoder
) extends CFMMOrdersParser[T2T_CFMM, ParserType.Default, F] {

  def deposit(box: Output): F[Option[CFMMOrder.DepositErgFee]] = {
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    val parsed: Option[CFMMOrder.DepositErgFee] =
      if (template == templates.depositV1) {
        for {
          poolId      <- tree.constants.parseBytea(13).map(PoolId.fromBytes)
          maxMinerFee <- tree.constants.parseLong(25)
          inX         <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount))
          inY         <- box.assets.lift(1).map(a => AssetAmount(a.tokenId, a.amount))
          dexFee      <- tree.constants.parseLong(15)
          redeemer    <- tree.constants.parsePk(0).map(pk => PubKey.fromBytes(pk.pkBytes))
          params = DepositParams(inX, inY, dexFee, redeemer)
        } yield CFMMOrder.DepositErgFee(poolId, maxMinerFee, ts, params, box)
      } else None
    parsed.pure
  }

  def redeem(box: Output): F[Option[CFMMOrder.RedeemErgFee]] = {
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    val parsed: Option[CFMMOrder.RedeemErgFee] =
      if (template == templates.redeemV1) {
        for {
          poolId      <- tree.constants.parseBytea(13).map(PoolId.fromBytes)
          maxMinerFee <- tree.constants.parseLong(19)
          inLP        <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount))
          dexFee      <- tree.constants.parseLong(15)
          redeemer    <- tree.constants.parsePk(0).map(pk => PubKey.fromBytes(pk.pkBytes))
          params = RedeemParams(inLP, dexFee, redeemer)
        } yield CFMMOrder.RedeemErgFee(poolId, maxMinerFee, ts, params, box)
      } else None
    parsed.pure
  }

  def swap(box: Output): F[Option[CFMMOrder.SwapAny]] = {
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    val parsed: Option[CFMMOrder.SwapAny] =
      if (template == templates.swapLatest) {
        for {
          poolId       <- tree.constants.parseBytea(14).map(PoolId.fromBytes)
          maxMinerFee  <- tree.constants.parseLong(21)
          inAmount     <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount))
          outId        <- tree.constants.parseBytea(2).map(TokenId.fromBytes)
          minOutAmount <- tree.constants.parseLong(15)
          outAmount = AssetAmount(outId, minOutAmount)
          dexFeePerTokenNum   <- tree.constants.parseLong(16)
          dexFeePerTokenDenom <- tree.constants.parseLong(17)
          redeemer            <- tree.constants.parsePk(0).map(pk => PubKey.fromBytes(pk.pkBytes))
          params = SwapParams(inAmount, outAmount, dexFeePerTokenNum, dexFeePerTokenDenom, redeemer)
        } yield CFMMOrder.SwapP2Pk(poolId, maxMinerFee, ts, params, box)
      } else None
    parsed.pure
  }
}

object T2TCFMMOrdersParserP2Pk {

  def make[F[_]: Monad: Clock](implicit e: ErgoAddressEncoder): CFMMOrdersParser[T2T_CFMM, ParserType.Default, F] =
    now.millis.map(ts => new T2TCFMMOrdersParserP2Pk(ts): CFMMOrdersParser[T2T_CFMM, ParserType.Default, F]).embed
}
