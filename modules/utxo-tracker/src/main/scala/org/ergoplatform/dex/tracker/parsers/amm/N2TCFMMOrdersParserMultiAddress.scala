package org.ergoplatform.dex.tracker.parsers.amm

import cats.effect.Clock
import cats.{Applicative, Monad}
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.protocol.ErgoTreeSerializer
import org.ergoplatform.dex.protocol.amm.AMMType.N2T_CFMM
import org.ergoplatform.dex.protocol.amm.{ParserType, T2TCFMMTemplates => templates}
import org.ergoplatform.ergo.domain.Output
import org.ergoplatform.ergo.syntax._
import org.ergoplatform.ergo.{ErgoTreeTemplate, SErgoTree, TokenId}
import tofu.syntax.embed._
import tofu.syntax.foption.noneF
import tofu.syntax.monadic._
import tofu.syntax.time.now

final class N2TCFMMOrdersParserMultiAddress[F[_]: Applicative: Clock](ts: Long)(implicit
  e: ErgoAddressEncoder
) extends CFMMOrdersParser[N2T_CFMM, ParserType.MultiAddress, F] {

  def deposit(box: Output): F[Option[CFMMOrder.Deposit]] = noneF

  def redeem(box: Output): F[Option[CFMMOrder.Redeem]] = noneF

  def swap(box: Output): F[Option[CFMMOrder.SwapAny]] = {
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree) //todo: check if tree -> address -> address tree
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
          params =
            SwapParams(
              inAmount,
              outAmount,
              dexFeePerTokenNum,
              dexFeePerTokenDenom,
              ErgoTreeSerializer.default.serialize(tree)
            ) // todo tree is incorrect!!!
        } yield CFMMOrder.SwapMultiAddress(poolId, maxMinerFee, ts, params, box)
      } else None
    parsed.pure
  }
}

object N2TCFMMOrdersParserMultiAddress {

  def make[F[_]: Monad: Clock](implicit e: ErgoAddressEncoder): CFMMOrdersParser[N2T_CFMM, ParserType.MultiAddress, F] =
    now.millis
      .map(ts => new N2TCFMMOrdersParserMultiAddress(ts): CFMMOrdersParser[N2T_CFMM, ParserType.MultiAddress, F])
      .embed
}
