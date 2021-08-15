package org.ergoplatform.dex.executor.orders.modules

import cats.data.NonEmptyList
import cats.instances.list._
import cats.syntax.option._
import cats.syntax.traverse._
import cats.{Functor, Monad}
import derevo.derive
import org.ergoplatform.ErgoBox._
import org.ergoplatform._
import org.ergoplatform.contracts.{DexBuyerContractParameters, DexLimitOrderContracts, DexSellerContractParameters}
import org.ergoplatform.dex.configs.ProtocolConfig
import org.ergoplatform.dex.domain.orderbook.FilledOrder._
import org.ergoplatform.dex.domain.orderbook.Trade
import org.ergoplatform.dex.domain.orderbook.Trade.AnyTrade
import org.ergoplatform.ergo.syntax._
import org.ergoplatform.dex.domain.syntax.trade._
import org.ergoplatform.dex.executor.orders.config.ExchangeConfig
import org.ergoplatform.dex.executor.orders.context.BlockchainContext
import org.ergoplatform.dex.executor.orders.domain.errors.ExecutionFailure
import org.ergoplatform.ergo.{TokenId, BoxId => DexBoxId}
import sigmastate.SType.AnyOps
import sigmastate.Values._
import sigmastate.eval._
import sigmastate.interpreter.ProverResult
import sigmastate.{SByte, SCollection, SLong, SType}
import tofu.Raise
import tofu.higherKind.Mid
import tofu.higherKind.derived.representableK
import tofu.syntax.context._
import tofu.syntax.embed._
import tofu.syntax.feither._
import tofu.syntax.monadic._

@derive(representableK)
trait TradeInterpreter[F[_]] {

  /** Interpret a given `trade` to a transaction.
    */
  def trade(trade: AnyTrade): F[ErgoLikeTransaction]
}

object TradeInterpreter {

  implicit def instance[
    F[_]: Monad: Raise[*[_], ExecutionFailure]: ExchangeConfig.Has: ProtocolConfig.Has: BlockchainContext.Has
  ]: TradeInterpreter[F] =
    (ExchangeConfig.access, ProtocolConfig.access, BlockchainContext.access).mapN {
      (dexConf, protoConf, blockchainCtx) =>
        NonEmptyList
          .of(new TransactionEnrichment[F](dexConf, protoConf), new DexOutputsCompaction[F](dexConf, protoConf))
          .reduce attach new Live[F](dexConf, protoConf, blockchainCtx)
    }.embed

  final class Live[F[_]: Monad: Raise[*[_], ExecutionFailure]: BlockchainContext.Has](
    exchangeConfig: ExchangeConfig,
    protocolConfig: ProtocolConfig,
    ctx: BlockchainContext
  )(implicit valueValidation: OutputValueValidation[F])
    extends TradeInterpreter[F] {

    implicit private val addressEncoder: ErgoAddressEncoder = protocolConfig.networkType.addressEncoder
    private val dexRewardProp                               = exchangeConfig.rewardAddress.toErgoTree

    def trade(trade: AnyTrade): F[ErgoLikeTransaction] = {
      val outsF =
        trade.refine match {
          case Right(Trade(order, counterOrders)) =>
            outputs(NonEmptyList.one(order), counterOrders)
          case Left(Trade(order, counterOrders)) =>
            outputs(counterOrders, NonEmptyList.one(order))
        }
      outsF.map { outs =>
        val ins = inputs(trade.orders)
        ErgoLikeTransaction(ins.toList.toVector, outs.toList.toVector)
      }
    }

    private def inputs(orders: NonEmptyList[AnyFilledOrder]): NonEmptyList[Input] =
      orders.map(ord => Input(ord.base.meta.boxId.toErgo, ProverResult.empty))

    private def outputs(
      asks: NonEmptyList[FilledAsk],
      bids: NonEmptyList[FilledBid]
    ): F[NonEmptyList[ErgoBoxCandidate]] = {
      val askAmount = asks.toList.map(_.base.amount).sum
      val bidAmount = bids.toList.map(_.base.amount).sum
      (executeAsks(asks.toList)(bidAmount), executeBids(bids.toList)(askAmount))
        .mapN((asks, bids) => NonEmptyList.fromListUnsafe(asks ++ bids))
    }

    private def executeAsks(
      asks: List[FilledAsk]
    )(amountToFill: Long, acc: List[ErgoBoxCandidate] = List.empty): F[List[ErgoBoxCandidate]] =
      asks match {
        case ask :: tl =>
          val desiredAmount   = ask.base.amount
          val amount          = desiredAmount min amountToFill
          val price           = ask.executionPrice
          val feePerToken     = ask.base.feePerToken
          val fee             = amount * feePerToken
          val rem             = ask.base.meta.boxValue - fee // todo: what if remainder is too small? Check
          val reward          = amount * price
          val sellerPk        = ask.base.meta.pk
          val dexFeeBox       = new ErgoBoxCandidate(fee, dexRewardProp, ctx.currentHeight)
          val returnRegisters = returnBoxRegs(ask.base.meta.boxId)
          if (desiredAmount <= amountToFill) {
            val returnAmount = reward + rem
            val returnBox =
              new ErgoBoxCandidate(returnAmount, sellerPk, ctx.currentHeight, additionalRegisters = returnRegisters)
            val outs = List(returnBox, dexFeeBox)
            outs.traverse(valueValidation.validate(_).absolve) >>
            executeAsks(tl)(amountToFill - amount, outs ++ acc)
          } else {
            val returnBox =
              new ErgoBoxCandidate(reward, sellerPk, ctx.currentHeight, additionalRegisters = returnRegisters)
            val assetId = ask.base.quoteAsset
            val residualParams =
              DexSellerContractParameters(ask.base.meta.pk, assetId.toErgo, ask.base.price, feePerToken)
            val residualScript    = DexLimitOrderContracts.sellerContractInstance(residualParams).ergoTree
            val unfilled          = ask.base.amount - amountToFill
            val residualTokens    = Colls.fromItems(assetId.toErgo -> unfilled)
            val residualRegisters = residualBoxRegs(assetId, price, feePerToken, ask.base.meta.boxId)
            val residualBox =
              new ErgoBoxCandidate(rem, residualScript, ctx.currentHeight, residualTokens, residualRegisters)
            val outs = List(residualBox, returnBox, dexFeeBox)
            outs.traverse(valueValidation.validate(_).absolve) >>
            executeAsks(tl)(amountToFill = 0L, outs ++ acc)
          }
        case Nil =>
          acc.pure[F]
      }

    private def executeBids(
      bids: List[FilledBid]
    )(amountToFill: Long, acc: List[ErgoBoxCandidate] = List.empty): F[List[ErgoBoxCandidate]] =
      bids match {
        case bid :: tl =>
          val desiredAmount   = bid.base.amount
          val amount          = desiredAmount min amountToFill
          val price           = bid.executionPrice
          val feePerToken     = bid.base.feePerToken
          val assetId         = bid.base.quoteAsset
          val dexFee          = amount * feePerToken
          val rem             = bid.base.meta.boxValue - dexFee - amount * price
          val reward          = amount
          val buyerPk         = bid.base.meta.pk
          val returnTokens    = Colls.fromItems(assetId.toErgo -> reward)
          val returnRegisters = returnBoxRegs(bid.base.meta.boxId)
          val fakeValue       = 99999L
          val returnBox       = new ErgoBoxCandidate(fakeValue, buyerPk, ctx.currentHeight, returnTokens, returnRegisters)
          if (desiredAmount <= amountToFill) {
            val minReturnBoxValue = (returnBox.bytesWithNoRef.length + 33) * ctx.nanoErgsPerByte
            val missingValue      = minReturnBoxValue - rem
            val dexFeeLeft        = dexFee - missingValue
            val returnBoxRefilled =
              new ErgoBoxCandidate(minReturnBoxValue, buyerPk, ctx.currentHeight, returnTokens, returnRegisters)
            val dexFeeBox = new ErgoBoxCandidate(dexFeeLeft, dexRewardProp, ctx.currentHeight)
            val outs      = List(returnBoxRefilled, dexFeeBox)
            outs.traverse(valueValidation.validate(_).absolve) >>
            executeBids(tl)(amountToFill - amount, outs ++ acc)
          } else {
            val residualParams =
              DexBuyerContractParameters(
                bid.base.meta.pk,
                assetId.toErgo,
                bid.base.price,
                feePerToken
              )
            val residualScript    = DexLimitOrderContracts.buyerContractInstance(residualParams).ergoTree
            val residualRegisters = residualBoxRegs(assetId, price, feePerToken, bid.base.meta.boxId)
            val residualBox =
              new ErgoBoxCandidate(rem, residualScript, ctx.currentHeight, Colls.emptyColl, residualRegisters)
            val dexFeeBox = new ErgoBoxCandidate(dexFee, dexRewardProp, ctx.currentHeight)
            val outs      = List(residualBox, returnBox, dexFeeBox)
            outs.traverse(valueValidation.validate(_).absolve) >>
            executeBids(tl)(amountToFill = 0L, outs ++ acc)
          }
        case Nil =>
          acc.pure[F]
      }

    private def returnBoxRegs(orderBoxId: DexBoxId): Map[NonMandatoryRegisterId, EvaluatedValue[SType]] =
      Map(R4 -> Constant(orderBoxId.toSigma.asWrappedType, SCollection(SByte)))

    private def residualBoxRegs(
      assetId: TokenId,
      price: Long,
      feePerToken: Long,
      originBoxId: DexBoxId
    ): Map[NonMandatoryRegisterId, EvaluatedValue[SType]] =
      Map(
        R4 -> Constant(assetId.toSigma.asWrappedType, SCollection(SByte)),
        R5 -> Constant(price.asWrappedType, SLong),
        R6 -> Constant(feePerToken.asWrappedType, SLong),
        R7 -> Constant(originBoxId.toSigma.asWrappedType, SCollection(SByte))
      )
  }

  /** An aspect merging dex reward outputs into single one.
    */
  final class DexOutputsCompaction[F[_]: Functor](exchangeConfig: ExchangeConfig, protocolConfig: ProtocolConfig)
    extends TradeInterpreter[Mid[F, *]] {

    implicit private val addressEncoder: ErgoAddressEncoder = protocolConfig.networkType.addressEncoder
    private val dexRewardProp                               = exchangeConfig.rewardAddress.toErgoTree

    def trade(trade: AnyTrade): Mid[F, ErgoLikeTransaction] =
      _.map(compact)

    private def compact(tx: ErgoLikeTransaction): ErgoLikeTransaction =
      tx.outputCandidates.partition(_.ergoTree == dexRewardProp) match {
        case (dexOuts, _) if dexOuts.length == 1 => tx
        case (dexOuts, otherOuts) =>
          val dexOutsMerged = dexOuts.foldLeft(none[ErgoBoxCandidate]) {
            case (None, out) => out.some
            case (Some(acc), out) =>
              new ErgoBoxCandidate(
                acc.value + out.value,
                acc.ergoTree,
                acc.creationHeight,
                acc.additionalTokens,
                acc.additionalRegisters
              ).some
          }
          val outs = dexOutsMerged ++ otherOuts
          new ErgoLikeTransaction(tx.inputs, tx.dataInputs, outs.toVector)
      }
  }

  /** An aspect merging dex reward outputs into single one.
    */
  final class TransactionEnrichment[F[_]: Functor](exchangeConfig: ExchangeConfig, protocolConfig: ProtocolConfig)
    extends TradeInterpreter[Mid[F, *]] {

    implicit private val addressEncoder: ErgoAddressEncoder = protocolConfig.networkType.addressEncoder
    private val dexRewardProp                               = exchangeConfig.rewardAddress.toErgoTree
    private val feeAddress                                  = Pay2SAddress(ErgoScriptPredef.feeProposition())

    def trade(trade: AnyTrade): Mid[F, ErgoLikeTransaction] =
      _.map(enrich)

    private def enrich(tx: ErgoLikeTransaction): ErgoLikeTransaction =
      tx.outputCandidates
        .find(_.ergoTree == dexRewardProp)
        .map { out =>
          val dexFeeOut = new ErgoBoxCandidate(
            out.value - exchangeConfig.executionFeeAmount,
            out.ergoTree,
            out.creationHeight,
            out.additionalTokens,
            out.additionalRegisters
          )
          val minerFeeOut =
            new ErgoBoxCandidate(exchangeConfig.executionFeeAmount, feeAddress.script, out.creationHeight)
          val remOuts = tx.outputs.filterNot(_.ergoTree == dexRewardProp)
          val outs    = dexFeeOut +: minerFeeOut +: remOuts
          new ErgoLikeTransaction(tx.inputs, tx.dataInputs, outs.toVector)
        }
        .getOrElse(tx)
  }
}
