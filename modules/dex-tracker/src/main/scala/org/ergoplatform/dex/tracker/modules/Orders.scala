package org.ergoplatform.dex.tracker.modules

import java.util

import cats.Monad
import cats.effect.Clock
import cats.syntax.option._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.contracts.DexLimitOrderContracts._
import org.ergoplatform.dex.domain.models.Order._
import org.ergoplatform.dex.domain.models.OrderMeta
import org.ergoplatform.dex.protocol.ErgoTreeSerializer
import org.ergoplatform.dex.protocol.models.{Asset, Output}
import org.ergoplatform.dex.tracker.domain.errors.{AssetNotProvided, BadParams, FeeNotSatisfied, OrderError}
import org.ergoplatform.dex.{constants, AssetId}
import sigmastate.Values.ErgoTree
import tofu.syntax.monadic._
import tofu.syntax.raise._
import tofu.syntax.time._
import tofu.{ApplicativeThrow, Raise}

trait Orders[F[_]] {

  def makeOrder(output: Output): F[Option[AnyOrder]]
}

object Orders {

  implicit def instance[F[_]: Monad: Clock: ApplicativeThrow](implicit
    addressEncoder: ErgoAddressEncoder
  ): Orders[F] = new Live[F]

  final private class Live[F[_]: Monad: Clock: Raise[*[_], OrderError]](implicit
    treeSer: ErgoTreeSerializer[F],
    addressEncoder: ErgoAddressEncoder
  ) extends Orders[F] {

    def makeOrder(output: Output): F[Option[AnyOrder]] =
      treeSer.deserialize(output.ergoTree).flatMap { tree =>
        if (isSellerScript(tree)) makeAsk(tree, output).map(_.some)
        else if (isBuyerScript(tree)) makeBid(tree, output).map(_.some)
        else none[AnyOrder].pure
      }

    private[tracker] def isSellerScript(tree: ErgoTree): Boolean =
      util.Arrays.equals(tree.template, sellerContractErgoTreeTemplate)

    private[tracker] def isBuyerScript(tree: ErgoTree): Boolean =
      util.Arrays.equals(tree.template, buyerContractErgoTreeTemplate)

    private[tracker] def makeAsk(tree: ErgoTree, output: Output): F[Ask] =
      for {
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

    private[tracker] def makeBid(tree: ErgoTree, output: Output): F[Bid] =
      for {
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
