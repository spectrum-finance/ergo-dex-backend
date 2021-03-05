package org.ergoplatform.dex.tracker.modules

import java.util

import cats.Monad
import cats.effect.Clock
import cats.syntax.option._
import derevo.derive
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.contracts.DexLimitOrderContracts._
import org.ergoplatform.dex.configs.ProtocolConfig
import org.ergoplatform.dex.domain.models.Order._
import org.ergoplatform.dex.domain.models.OrderMeta
import org.ergoplatform.dex.clients.explorer.models.{Asset, Output}
import org.ergoplatform.dex.protocol.ErgoTreeSerializer
import org.ergoplatform.dex.tracker.domain.errors._
import org.ergoplatform.dex.{constants, AssetId}
import sigmastate.Values.ErgoTree
import tofu.higherKind.derived.representableK
import tofu.syntax.context._
import tofu.syntax.embed._
import tofu.syntax.monadic._
import tofu.syntax.raise._
import tofu.syntax.time._
import tofu.{MonadThrow, Raise, WithContext}

@derive(representableK)
trait Orders[F[_]] {

  def makeOrder(output: Output): F[Option[AnyOrder]]
}

object Orders {

  implicit def instance[F[_]: Clock: MonadThrow: ProtocolConfig.Has]: Orders[F] =
    context.map { conf =>
      val ser     = ErgoTreeSerializer.default
      val encoder = conf.networkType.addressEncoder
      new Live[F](ser, encoder): Orders[F]
    }.embed

  final private class Live[F[_]: Monad: Clock: Raise[*[_], InvalidOrder]](
    treeSer: ErgoTreeSerializer,
    addressEncoder: ErgoAddressEncoder
  ) extends Orders[F] {

    implicit val e: ErgoAddressEncoder = addressEncoder

    def makeOrder(output: Output): F[Option[AnyOrder]] = {
      val tree = treeSer.deserialize(output.ergoTree)
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
                    .collectFirst { case a if a.tokenId == quoteAsset => a.amount }
                    .orRaise(AssetNotProvided(quoteAsset))
        price       = params.tokenPrice
        feePerToken = params.dexFeePerToken
        minValue    = feePerToken * amount
        _  <- if (output.value < minValue) FeeNotSatisfied(output.value, minValue).raise else unit
        ts <- now.millis
        meta = OrderMeta(output.boxId, output.value, tree, params.sellerPk, ts)
      } yield mkAsk(quoteAsset, baseAsset, amount, price, feePerToken, meta)

    private[tracker] def makeBid(tree: ErgoTree, output: Output): F[Bid] =
      for {
        params <- parseBuyerContractParameters(tree).orRaise(BadParams(tree))
        _ <- if (output.value % (params.tokenPrice + params.dexFeePerToken) != 0)
               InvalidBidValue(output.value, params.tokenPrice, params.dexFeePerToken).raise
             else unit
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
