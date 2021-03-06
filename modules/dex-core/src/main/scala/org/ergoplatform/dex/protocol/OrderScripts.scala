package org.ergoplatform.dex.protocol

import mouse.any._
import org.ergoplatform.contracts.DexLimitOrderContracts._
import org.ergoplatform.dex.clients.explorer.models.ErgoBox
import org.ergoplatform.dex.domain.models.OrderType
import org.ergoplatform.dex.{AssetId, HexString, SErgoTree}
import sigmastate.Values.{ErgoTree, SigmaPropConstant}
import sigmastate.basics.DLogProtocol.ProveDlog

trait OrderScripts[CT <: ContractType] {

  def isAsk(ergoTree: SErgoTree): Boolean

  def isBid(ergoTree: SErgoTree): Boolean

  def parseAsk(box: ErgoBox): Option[OrderParams]

  def parseBid(box: ErgoBox): Option[OrderParams]
}

object OrderScripts {

  implicit def limitOrderInstance(implicit
    templates: ScriptTemplates[ContractType.LimitOrder]
  ): OrderScripts[ContractType.LimitOrder] =
    new OrderScripts[ContractType.LimitOrder] {

      def isAsk(ergoTree: SErgoTree): Boolean =
        HexString.fromBytes(ErgoTreeSerializer.default.deserialize(ergoTree).template) ==
          templates.ask

      def isBid(ergoTree: SErgoTree): Boolean =
        HexString.fromBytes(ErgoTreeSerializer.default.deserialize(ergoTree).template) ==
          templates.bid

      def parseAsk(box: ErgoBox): Option[OrderParams] =
        (ErgoTreeSerializer.default.deserialize(box.ergoTree) |> parseSellerContractParameters)
          .flatMap { params =>
            val quote = AssetId.fromBytes(params.tokenId)
            box.assets.find(_.tokenId == quote).map { asset =>
              OrderParams(
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
            OrderParams(
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
