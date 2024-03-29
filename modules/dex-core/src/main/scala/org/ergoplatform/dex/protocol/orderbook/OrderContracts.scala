package org.ergoplatform.dex.protocol.orderbook

import mouse.any._
import org.ergoplatform.contracts.DexLimitOrderContracts._
import org.ergoplatform.dex.domain.orderbook.OrderType
import org.ergoplatform.dex.protocol.{ErgoTreeSerializer, constants, orderbook}
import org.ergoplatform.ergo.{ErgoTreeTemplate, SErgoTree, TokenId}
import org.ergoplatform.ergo.domain.Output
import sigmastate.Values.{ErgoTree, SigmaPropConstant}
import sigmastate.basics.DLogProtocol.ProveDlog

trait OrderContracts[CT <: OrderContractFamily] {

  def isAsk(ergoTree: SErgoTree): Boolean

  def isBid(ergoTree: SErgoTree): Boolean

  def parseAsk(box: Output): Option[OrderParams]

  def parseBid(box: Output): Option[OrderParams]
}

object OrderContracts {

  implicit def limitOrderInstance(implicit
    templates: ContractTemplates[OrderContractFamily.LimitOrders]
  ): OrderContracts[OrderContractFamily.LimitOrders] =
    new OrderContracts[OrderContractFamily.LimitOrders] {

      def isAsk(ergoTree: SErgoTree): Boolean =
        ErgoTreeTemplate.fromBytes(ErgoTreeSerializer.default.deserialize(ergoTree).template) ==
          templates.ask

      def isBid(ergoTree: SErgoTree): Boolean =
        ErgoTreeTemplate.fromBytes(ErgoTreeSerializer.default.deserialize(ergoTree).template) ==
          templates.bid

      def parseAsk(box: Output): Option[OrderParams] =
        (ErgoTreeSerializer.default.deserialize(box.ergoTree) |> parseSellerContractParameters)
          .flatMap { params =>
            val quote = TokenId.fromBytes(params.tokenId)
            box.assets.find(_.tokenId == quote).map { asset =>
              orderbook.OrderParams(
                orderType     = OrderType.Ask,
                baseAsset     = constants.ErgoAssetId,
                quoteAsset    = quote,
                amount        = asset.amount,
                price         = params.tokenPrice,
                feePerToken   = params.dexFeePerToken,
                ownerErgoTree = deriveErgoTree(params.sellerPk)
              )
            }
          }

      def parseBid(box: Output): Option[OrderParams] =
        (ErgoTreeSerializer.default.deserialize(box.ergoTree) |> parseBuyerContractParameters)
          .map { params =>
            orderbook.OrderParams(
              orderType     = OrderType.Ask,
              baseAsset     = constants.ErgoAssetId,
              quoteAsset    = TokenId.fromBytes(params.tokenId),
              amount        = box.value / params.tokenPrice,
              price         = params.tokenPrice,
              feePerToken   = params.dexFeePerToken,
              ownerErgoTree = deriveErgoTree(params.buyerPk)
            )
          }

      private def deriveErgoTree(pk: ProveDlog) =
        ErgoTreeSerializer.default.serialize(
          ErgoTree(ErgoTree.DefaultHeader, ErgoTree.EmptyConstants, SigmaPropConstant(pk))
        )
    }
}
