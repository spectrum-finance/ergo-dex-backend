package org.ergoplatform.dex.protocol.orderbook

import mouse.any._
import org.ergoplatform.contracts.DexLimitOrderContracts._
import org.ergoplatform.dex.clients.explorer.models.ErgoBox
import org.ergoplatform.dex.domain.orderbook.OrderType
import org.ergoplatform.dex.protocol.{constants, orderbook, ErgoTreeSerializer}
import org.ergoplatform.dex.{AssetId, ErgoTreeTemplate, SErgoTree}
import sigmastate.Values.{ErgoTree, SigmaPropConstant}
import sigmastate.basics.DLogProtocol.ProveDlog

trait OrderContracts[CT <: OrderContractType] {

  def isAsk(ergoTree: SErgoTree): Boolean

  def isBid(ergoTree: SErgoTree): Boolean

  def parseAsk(box: ErgoBox): Option[OrderParams]

  def parseBid(box: ErgoBox): Option[OrderParams]
}

object OrderContracts {

  implicit def limitOrderInstance(implicit
    templates: ContractTemplates[OrderContractType.LimitOrder]
  ): OrderContracts[OrderContractType.LimitOrder] =
    new OrderContracts[OrderContractType.LimitOrder] {

      def isAsk(ergoTree: SErgoTree): Boolean =
        ErgoTreeTemplate.fromBytes(ErgoTreeSerializer.default.deserialize(ergoTree).template) ==
          templates.ask

      def isBid(ergoTree: SErgoTree): Boolean =
        ErgoTreeTemplate.fromBytes(ErgoTreeSerializer.default.deserialize(ergoTree).template) ==
          templates.bid

      def parseAsk(box: ErgoBox): Option[OrderParams] =
        (ErgoTreeSerializer.default.deserialize(box.ergoTree) |> parseSellerContractParameters)
          .flatMap { params =>
            val quote = AssetId.fromBytes(params.tokenId)
            box.assets.find(_.tokenId == quote).map { asset =>
              orderbook.OrderParams(
                orderType     = OrderType.Ask,
                baseAsset     = constants.NativeAssetId,
                quoteAsset    = quote,
                amount        = asset.amount,
                price         = params.tokenPrice,
                feePerToken   = params.dexFeePerToken,
                ownerErgoTree = deriveErgoTree(params.sellerPk)
              )
            }
          }

      def parseBid(box: ErgoBox): Option[OrderParams] =
        (ErgoTreeSerializer.default.deserialize(box.ergoTree) |> parseBuyerContractParameters)
          .map { params =>
            orderbook.OrderParams(
              orderType     = OrderType.Ask,
              baseAsset     = constants.NativeAssetId,
              quoteAsset    = AssetId.fromBytes(params.tokenId),
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
