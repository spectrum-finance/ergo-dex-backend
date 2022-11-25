package org.ergoplatform.dex.tracker.parsers.amm.analytics

import cats.effect.Clock
import cats.{Applicative, Monad}
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.amm.CFMMVersionedOrder._
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.protocol.ErgoTreeSerializer
import org.ergoplatform.dex.protocol.amm.AMMType.T2T_CFMM
import org.ergoplatform.dex.protocol.amm.{T2TCFMMTemplates => templates}
import org.ergoplatform.ergo.domain.Output
import org.ergoplatform.ergo.syntax._
import org.ergoplatform.ergo.{ErgoTreeTemplate, PubKey, TokenId}
import tofu.syntax.embed._
import tofu.syntax.monadic._
import tofu.syntax.time.now

final class T2TCFMMOrdersLegacyContractsParser[F[_]: Applicative: Clock](ts: Long)(implicit
                                                                                   e: ErgoAddressEncoder
) extends LegacyContractsParser[T2T_CFMM, F] {

  def depositV1(box: Output): F[Option[DepositV1]] = {
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    val parsed =
      if (template == templates.depositLegacyV1) {
        for {
          poolId   <- tree.constants.parseBytea(13).map(PoolId.fromBytes)
          inX      <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount))
          inY      <- box.assets.lift(1).map(a => AssetAmount(a.tokenId, a.amount))
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
          inX      <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount))
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
          poolId   <- tree.constants.parseBytea(13).map(PoolId.fromBytes)
          inLP     <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount))
          dexFee   <- tree.constants.parseLong(15)
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
      if (template == templates.swapLegacyV0) {
        for {
          poolId       <- tree.constants.parseBytea(14).map(PoolId.fromBytes)
          inAmount     <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount))
          outId        <- tree.constants.parseBytea(2).map(TokenId.fromBytes)
          minOutAmount <- tree.constants.parseLong(15)
          outAmount = AssetAmount(outId, minOutAmount)
          dexFeePerTokenNum   <- tree.constants.parseLong(16)
          dexFeePerTokenDenom <- tree.constants.parseLong(17)
          redeemer            <- tree.constants.parsePk(0).map(pk => PubKey.fromBytes(pk.pkBytes))
          params = SwapParams(inAmount, outAmount, dexFeePerTokenNum, dexFeePerTokenDenom, redeemer)
        } yield SwapV0(poolId, ts, params, box)
      } else None
    parsed.pure
  }
}

object T2TCFMMOrdersLegacyContractsParser {

  def make[F[_]: Monad: Clock](implicit e: ErgoAddressEncoder): LegacyContractsParser[T2T_CFMM, F] =
    now.millis.map(ts => new T2TCFMMOrdersLegacyContractsParser(ts): LegacyContractsParser[T2T_CFMM, F]).embed
}
