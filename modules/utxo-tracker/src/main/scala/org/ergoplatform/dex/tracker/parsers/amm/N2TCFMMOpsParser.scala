package org.ergoplatform.dex.tracker.parsers.amm

import cats.{Applicative, Monad}
import cats.effect.Clock
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.protocol.ErgoTreeSerializer
import org.ergoplatform.dex.protocol.amm.AMMType.N2T_CFMM
import org.ergoplatform.dex.protocol.amm.{N2TCFMMTemplates => templates}
import org.ergoplatform.ergo.models.Output
import org.ergoplatform.ergo.syntax._
import org.ergoplatform.ergo.{Address, ErgoTreeTemplate, TokenId}
import org.ergoplatform.{ErgoAddressEncoder, P2PKAddress}
import sigmastate.Values.ErgoTree
import tofu.syntax.monadic._
import tofu.syntax.time._
import tofu.syntax.embed._

final class N2TCFMMOpsParser[F[_]: Applicative](ts: Long)(implicit
  e: ErgoAddressEncoder
) extends AMMOpsParser[N2T_CFMM, F] {

  def deposit(box: Output): F[Option[Deposit]] = {
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    val parsed =
      if (template == templates.deposit) {
        for {
          poolId <- tree.constants.parseBytea(9).map(PoolId.fromBytes)
          inX = AssetAmount.native(box.value)
          inY    <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount, a.name))
          dexFee <- tree.constants.parseLong(10)
          p2pk   <- tree.constants.parsePk(0).map(pk => Address.fromStringUnsafe(P2PKAddress(pk).toString))
          params = DepositParams(inX, inY, dexFee, p2pk)
        } yield Deposit(poolId, ts, params, box)
      } else None
    parsed.pure
  }

  def redeem(box: Output): F[Option[Redeem]] = {
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    val parsed =
      if (template == templates.redeem) {
        for {
          poolId <- tree.constants.parseBytea(11).map(PoolId.fromBytes)
          inLP   <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount, a.name))
          dexFee <- tree.constants.parseLong(12)
          p2pk   <- tree.constants.parsePk(0).map(pk => Address.fromStringUnsafe(P2PKAddress(pk).toString))
          params = RedeemParams(inLP, dexFee, p2pk)
        } yield Redeem(poolId, ts, params, box)
      } else None
    parsed.pure
  }

  def swap(box: Output): F[Option[Swap]] = {
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    val parsed =
      if (template == templates.swapSell) swapSell(box, tree)
      else if (template == templates.swapBuy) swapBuy(box, tree)
      else None
    parsed.pure
  }

  private def swapSell(box: Output, tree: ErgoTree) =
    for {
      poolId     <- tree.constants.parseBytea(7).map(PoolId.fromBytes)
      baseAmount <- tree.constants.parseLong(11)
      inAmount = AssetAmount.native(baseAmount)
      outId        <- tree.constants.parseBytea(8).map(TokenId.fromBytes)
      minOutAmount <- tree.constants.parseLong(9)
      outAmount = AssetAmount(outId, minOutAmount, None)
      dexFeePerTokenNum   <- tree.constants.parseLong(15)
      dexFeePerTokenDenom <- tree.constants.parseLong(16)
      p2pk                <- tree.constants.parsePk(0).map(pk => Address.fromStringUnsafe(P2PKAddress(pk).toString))
      params = SwapParams(inAmount, outAmount, dexFeePerTokenNum, dexFeePerTokenDenom, p2pk)
    } yield Swap(poolId, ts, params, box)

  private def swapBuy(box: Output, tree: ErgoTree) =
    for {
      poolId       <- tree.constants.parseBytea(9).map(PoolId.fromBytes)
      inAmount     <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount, a.name))
      minOutAmount <- tree.constants.parseLong(10)
      outAmount = AssetAmount.native(minOutAmount)
      dexFeePerTokenNum   <- tree.constants.parseLong(12)
      dexFeePerTokenDenom <- tree.constants.parseLong(5)
      p2pk                <- tree.constants.parsePk(0).map(pk => Address.fromStringUnsafe(P2PKAddress(pk).toString))
      params = SwapParams(inAmount, outAmount, dexFeePerTokenNum, dexFeePerTokenDenom, p2pk)
    } yield Swap(poolId, ts, params, box)
}

object N2TCFMMOpsParser {
  def make[F[_]: Monad: Clock](implicit e: ErgoAddressEncoder): AMMOpsParser[N2T_CFMM, F] =
    now.millis.map(ts => new N2TCFMMOpsParser(ts): AMMOpsParser[N2T_CFMM, F]).embed
}
