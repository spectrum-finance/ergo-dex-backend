package org.ergoplatform.dex.tracker.parsers.amm

import cats.effect.Clock
import cats.{Applicative, Monad}
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.protocol.ErgoTreeSerializer
import org.ergoplatform.dex.protocol.amm.AMMType.T2T_CFMM
import org.ergoplatform.dex.protocol.amm.{T2TCFMMTemplates => templates}
import org.ergoplatform.ergo.models.Output
import org.ergoplatform.ergo.syntax._
import org.ergoplatform.ergo.{Address, ErgoTreeTemplate, TokenId}
import org.ergoplatform.{ErgoAddressEncoder, P2PKAddress}
import tofu.syntax.embed._
import tofu.syntax.monadic._
import tofu.syntax.time.now

final class T2TCFMMOrdersParser[F[_]: Applicative: Clock](ts: Long)(implicit
  e: ErgoAddressEncoder
) extends CFMMOrdersParser[T2T_CFMM, F] {

  def deposit(box: Output): F[Option[Deposit]] = {
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    val parsed =
      if (template == templates.deposit) {
        for {
          poolId      <- tree.constants.parseBytea(13).map(PoolId.fromBytes)
          maxMinerFee <- tree.constants.parseLong(25)
          inX         <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount))
          inY         <- box.assets.lift(1).map(a => AssetAmount(a.tokenId, a.amount))
          dexFee      <- tree.constants.parseLong(15)
          p2pk        <- tree.constants.parsePk(0).map(pk => Address.fromStringUnsafe(P2PKAddress(pk).toString))
          params = DepositParams(inX, inY, dexFee, p2pk)
        } yield Deposit(poolId, maxMinerFee, ts, params, box)
      } else None
    parsed.pure
  }

  def redeem(box: Output): F[Option[Redeem]] = {
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    val parsed =
      if (template == templates.redeem) {
        for {
          poolId      <- tree.constants.parseBytea(13).map(PoolId.fromBytes)
          maxMinerFee <- tree.constants.parseLong(19)
          inLP        <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount))
          dexFee      <- tree.constants.parseLong(15)
          p2pk        <- tree.constants.parsePk(0).map(pk => Address.fromStringUnsafe(P2PKAddress(pk).toString))
          params = RedeemParams(inLP, dexFee, p2pk)
        } yield Redeem(poolId, maxMinerFee, ts, params, box)
      } else None
    parsed.pure
  }

  def swap(box: Output): F[Option[Swap]] = {
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    val parsed =
      if (template == templates.swap) {
        for {
          poolId       <- tree.constants.parseBytea(14).map(PoolId.fromBytes)
          maxMinerFee  <- tree.constants.parseLong(21)
          inAmount     <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount))
          outId        <- tree.constants.parseBytea(2).map(TokenId.fromBytes)
          minOutAmount <- tree.constants.parseLong(15)
          outAmount = AssetAmount(outId, minOutAmount)
          dexFeePerTokenNum   <- tree.constants.parseLong(16)
          dexFeePerTokenDenom <- tree.constants.parseLong(17)
          p2pk                <- tree.constants.parsePk(0).map(pk => Address.fromStringUnsafe(P2PKAddress(pk).toString))
          params = SwapParams(inAmount, outAmount, dexFeePerTokenNum, dexFeePerTokenDenom, p2pk)
        } yield Swap(poolId, maxMinerFee, ts, params, box)
      } else None
    parsed.pure
  }
}

object T2TCFMMOrdersParser {

  def make[F[_]: Monad: Clock](implicit e: ErgoAddressEncoder): CFMMOrdersParser[T2T_CFMM, F] =
    now.millis.map(ts => new T2TCFMMOrdersParser(ts): CFMMOrdersParser[T2T_CFMM, F]).embed
}
