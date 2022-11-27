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
import org.ergoplatform.ergo.{ErgoTreeTemplate, SErgoTree, TokenId}
import tofu.syntax.embed._
import tofu.syntax.foption.noneF
import tofu.syntax.monadic._
import tofu.syntax.time.now

final class T2TCFMMOrdersParserMultiAddress[F[_]: Applicative: Clock](ts: Long)(implicit
  e: ErgoAddressEncoder
) extends CFMMOrdersParser[T2T_CFMM, ParserType.MultiAddress, F] {

  def deposit(box: Output): F[Option[CFMMOrder.DepositErgFee]] = noneF

  def redeem(box: Output): F[Option[CFMMOrder.Redeem]] = noneF

  def swap(box: Output): F[Option[CFMMOrder.SwapAny]] = {
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    val parsed: Option[CFMMOrder.SwapAny] =
      if (template == templates.swapV2) {
        for {
          poolId       <- tree.constants.parseBytea(14).map(PoolId.fromBytes)
          maxMinerFee  <- tree.constants.parseLong(22)
          inAmount     <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount))
          outId        <- tree.constants.parseBytea(1).map(TokenId.fromBytes)
          minOutAmount <- tree.constants.parseLong(16)
          outAmount = AssetAmount(outId, minOutAmount)
          dexFeePerTokenNum   <- tree.constants.parseLong(17)
          dexFeePerTokenDenom <- tree.constants.parseLong(18)
          redeemer            <- tree.constants.parseBytea(15).map(SErgoTree.fromBytes)
          params = SwapParams(
                     inAmount,
                     outAmount,
                     dexFeePerTokenNum,
                     dexFeePerTokenDenom,
                     redeemer
                   )
        } yield CFMMOrder.SwapMultiAddress(poolId, maxMinerFee, ts, params, box)
      } else None
    parsed.pure
  }
}

object T2TCFMMOrdersParserMultiAddress {

  def make[F[_]: Monad: Clock](implicit e: ErgoAddressEncoder): CFMMOrdersParser[T2T_CFMM, ParserType.MultiAddress, F] =
    now.millis
      .map(ts => new T2TCFMMOrdersParserMultiAddress(ts): CFMMOrdersParser[T2T_CFMM, ParserType.MultiAddress, F])
      .embed
}
