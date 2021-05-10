package org.ergoplatform.dex.tracker.modules.orders

import cats.Monad
import cats.effect.Clock
import cats.syntax.option._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.contracts.DexLimitOrderContracts._
import org.ergoplatform.dex.TokenId
import org.ergoplatform.dex.configs.ProtocolConfig
import org.ergoplatform.dex.domain.network.Output
import org.ergoplatform.dex.domain.orderbook.Order._
import org.ergoplatform.dex.domain.orderbook.OrderMeta
import org.ergoplatform.dex.protocol.orderbook.OrderContractFamily
import org.ergoplatform.dex.protocol.orderbook.OrderContractFamily.LimitOrders
import org.ergoplatform.dex.protocol.{ErgoTreeSerializer, constants}
import org.ergoplatform.dex.tracker.domain.errors._
import sigmastate.Values.ErgoTree
import tofu.higherKind.RepresentableK
import tofu.syntax.context._
import tofu.syntax.embed._
import tofu.syntax.monadic._
import tofu.syntax.raise._
import tofu.syntax.time._
import tofu.{MonadThrow, Raise}

import java.util

trait OrdersOps[CT <: OrderContractFamily, F[_]] {

  def parseOrder(output: Output): F[Option[AnyOrder]]
}

object OrdersOps {

  implicit def representableK[CT <: OrderContractFamily]: RepresentableK[OrdersOps[CT, *[_]]] = {
    type Rep[F[_]] = OrdersOps[CT, F]
    tofu.higherKind.derived.genRepresentableK[Rep]
  }

  implicit def limitOrderInstance[F[_]: Clock: MonadThrow: ProtocolConfig.Has]: OrdersOps[LimitOrders, F] =
    context.map { conf =>
      val ser     = ErgoTreeSerializer.default
      val encoder = conf.networkType.addressEncoder
      new Live[F](ser, encoder): OrdersOps[LimitOrders, F]
    }.embed

  final private class Live[F[_]: Monad: Clock: Raise[*[_], InvalidOrder]](
    treeSer: ErgoTreeSerializer,
    addressEncoder: ErgoAddressEncoder
  ) extends OrdersOps[LimitOrders, F] {

    implicit val e: ErgoAddressEncoder = addressEncoder

    def parseOrder(output: Output): F[Option[AnyOrder]] = {
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
        baseAsset  = constants.NativeAssetId
        quoteAsset = TokenId.fromBytes(params.tokenId)
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
        baseAsset   = constants.NativeAssetId
        quoteAsset  = TokenId.fromBytes(params.tokenId)
        price       = params.tokenPrice
        feePerToken = params.dexFeePerToken
        amount      = output.value / (price + feePerToken)
        ts <- now.millis
        meta = OrderMeta(output.boxId, output.value, tree, params.buyerPk, ts)
      } yield mkBid(quoteAsset, baseAsset, amount, price, feePerToken, meta)
  }
}
