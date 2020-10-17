package org.ergoplatform.dex.watcher

import cats.Monad
import cats.effect.Clock
import cats.syntax.option._
import org.ergoplatform.{ErgoAddressEncoder, P2PKAddress}
import org.ergoplatform.contracts.DexLimitOrderContracts._
import org.ergoplatform.dex.{constants, AssetId, HexString}
import org.ergoplatform.dex.domain.models.Order._
import org.ergoplatform.dex.domain.models.OrderMeta
import org.ergoplatform.dex.protocol.ErgoTree
import org.ergoplatform.dex.protocol.models.{Asset, Output}
import org.ergoplatform.dex.watcher.domain.errors.{AssetNotProvided, BadParams, FeeNotSatisfied, OrderError}
import tofu.Raise
import tofu.syntax.monadic._
import tofu.syntax.raise._
import tofu.syntax.time._

trait Orders[F[_]] {

  def makeOrder(output: Output): F[Option[AnyOrder]]
}

object Orders {

  final private class Live[F[_]: Monad: Clock: Raise[*[_], OrderError]](implicit
    ergoTree: ErgoTree[F],
    addressEncoder: ErgoAddressEncoder
  ) extends Orders[F] {

    def makeOrder(output: Output): F[Option[AnyOrder]] =
      if (isAsk(output.ergoTree)) makeAsk(output).map(_.some)
      else if (isBid(output.ergoTree)) makeBid(output).map(_.some)
      else none[AnyOrder].pure

    private[watcher] def isAsk(ergoTree: HexString): Boolean = ???

    private[watcher] def isBid(ergoTree: HexString): Boolean = ???

    private[watcher] def makeAsk(output: Output): F[Ask] =
      for {
        tree   <- ergoTree.parse(output.ergoTree)
        params <- parseSellerContractParameters(tree).orRaise(BadParams(tree))
        baseAsset  = constants.ErgoAssetId
        quoteAsset = AssetId.fromBytes(params.tokenId)
        amount <- output.assets
                    .collectFirst { case Asset(tokenId, amount) if tokenId == quoteAsset => amount }
                    .orRaise(AssetNotProvided(quoteAsset))
        price       = params.tokenPrice
        feePerToken = params.dexFeePerToken
        minValue    = feePerToken * amount
        _  <- if (output.value < minValue) FeeNotSatisfied(output.value, minValue).raise else ().pure
        ts <- now.millis
        meta = OrderMeta(output.boxId, output.value, tree, params.sellerPk, ts)
      } yield mkAsk(quoteAsset, baseAsset, amount, price, feePerToken, meta)

    private[watcher] def makeBid(output: Output): F[Bid] =
      for {
        tree   <- ergoTree.parse(output.ergoTree)
        params <- parseBuyerContractParameters(tree).orRaise(BadParams(tree))
        baseAsset   = constants.ErgoAssetId
        quoteAsset  = AssetId.fromBytes(params.tokenId)
        price       = params.tokenPrice
        feePerToken = params.dexFeePerToken
        amount      = output.value / (price + feePerToken)
        ts <- now.millis
        meta = OrderMeta(output.boxId, output.value, tree, params.buyerPk, ts)
      } yield mkBid(quoteAsset, baseAsset, amount, price, feePerToken, meta)
  }
}
