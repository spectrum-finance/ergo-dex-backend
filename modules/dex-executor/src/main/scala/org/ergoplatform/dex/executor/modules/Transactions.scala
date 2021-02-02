package org.ergoplatform.dex.executor.modules

import cats.data.NonEmptyList
import cats.syntax.option._
import cats.{Applicative, Functor, Monad}
import derevo.derive
import org.ergoplatform.ErgoBox._
import org.ergoplatform.contracts.{DexBuyerContractParameters, DexLimitOrderContracts, DexSellerContractParameters}
import org.ergoplatform.dex.configs.ProtocolConfig
import org.ergoplatform.dex.domain.models.FilledOrder._
import org.ergoplatform.dex.domain.models.Trade
import org.ergoplatform.dex.domain.models.Trade.AnyTrade
import org.ergoplatform.dex.domain.syntax.ergo._
import org.ergoplatform.dex.domain.syntax.trade._
import org.ergoplatform.dex.executor.config.ExchangeConfig
import org.ergoplatform.dex.executor.context.BlockchainContext
import org.ergoplatform.{ErgoAddressEncoder, ErgoBoxCandidate, ErgoLikeTransaction, Input}
import sigmastate.SType.AnyOps
import sigmastate.Values._
import sigmastate.eval._
import sigmastate.interpreter.ProverResult
import sigmastate.{SByte, SCollection, SLong, SType}
import tofu.WithContext
import tofu.higherKind.Mid
import tofu.higherKind.derived.representableK
import tofu.syntax.context._
import tofu.syntax.embed._
import tofu.syntax.monadic._

import scala.annotation.tailrec

@derive(representableK)
trait Transactions[F[_]] {

  /** Translate a given `trade` to executing transaction.
    */
  def translate(trade: AnyTrade): F[ErgoLikeTransaction]
}

object Transactions {

  // format: off
  implicit def apply[
    F[_]: Monad: WithContext[*[_], ExchangeConfig]
        : WithContext[*[_], ProtocolConfig]: WithContext[*[_], BlockchainContext]
  ]: Transactions[F] =
    (hasContext[F, ExchangeConfig], hasContext[F, ProtocolConfig], hasContext[F, BlockchainContext])
      .mapN { (dexConf, protoConf, blockchainCtx) =>
        new DexOutputsCompaction[F](dexConf, protoConf) attach new Live[F](dexConf, protoConf, blockchainCtx)
      }.embed
  // format: on

  // todo: Trade execution may fail because of low output values.
  // Need a mechanism to return failed trades to the matcher.
  final class Live[F[_]: Applicative](
    exchangeConfig: ExchangeConfig,
    protocolConfig: ProtocolConfig,
    ctx: BlockchainContext
  ) extends Transactions[F] {

    implicit private val addressEncoder: ErgoAddressEncoder = protocolConfig.networkType.addressEncoder
    private val dexRewardProp                               = exchangeConfig.rewardAddress.toErgoTree

    def translate(trade: AnyTrade): F[ErgoLikeTransaction] = {
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
      val outputs   = executeAsks(asks.toList)(bidAmount) ++ executeBids(bids.toList)(askAmount)
      NonEmptyList.fromListUnsafe(outputs).pure
    }

    @tailrec private def executeAsks(
      asks: List[FilledAsk]
    )(toFill: Long, acc: List[ErgoBoxCandidate] = List.empty): List[ErgoBoxCandidate] =
      asks match {
        case ask :: tl if ask.base.amount <= toFill =>
          val amount      = ask.base.amount
          val price       = ask.executionPrice
          val feePerToken = ask.base.feePerToken
          val fee         = amount * feePerToken
          val rem         = ask.base.meta.boxValue - fee // todo: what if remainder is too small? Check
          val reward      = amount * price
          val pk          = ask.base.meta.pk
          val dexFeeBox   = new ErgoBoxCandidate(fee, dexRewardProp, ctx.currentHeight)
          val returnRegisters = Map(
            R4 -> Constant(ask.base.meta.boxId.toSigma.asWrappedType, SCollection(SByte))
          ).asInstanceOf[Map[NonMandatoryRegisterId, EvaluatedValue[SType]]]
          val returnBox =
            new ErgoBoxCandidate(reward + rem, pk, ctx.currentHeight, additionalRegisters = returnRegisters)
          executeAsks(tl)(toFill - amount, returnBox +: dexFeeBox +: acc)
        case ask :: tl =>
          val amount      = toFill
          val price       = ask.executionPrice
          val feePerToken = ask.base.feePerToken
          val assetId     = ask.base.quoteAsset
          val fee         = amount * feePerToken
          val rem         = ask.base.meta.boxValue - fee // todo: what if remainder is too small? Check
          val reward      = amount * price
          val pk          = ask.base.meta.pk
          val unfilled    = ask.base.amount - toFill
          val dexFeeBox   = new ErgoBoxCandidate(fee, dexRewardProp, ctx.currentHeight)
          val returnRegisters = Map(
            R4 -> Constant(ask.base.meta.boxId.toSigma.asWrappedType, SCollection(SByte))
          ).asInstanceOf[Map[NonMandatoryRegisterId, EvaluatedValue[SType]]]
          val returnBox = new ErgoBoxCandidate(reward, pk, ctx.currentHeight, additionalRegisters = returnRegisters)
          val residualParams =
            DexSellerContractParameters(
              ask.base.meta.pk,
              assetId.toErgo,
              ask.base.price,
              feePerToken
            )
          val residualScript = DexLimitOrderContracts.sellerContractInstance(residualParams).ergoTree
          val residualTokens = Colls.fromItems(assetId.toErgo -> unfilled)
          val residualRegisters = Map(
            R4 -> Constant(assetId.toSigma.asWrappedType, SCollection(SByte)),
            R5 -> Constant(price.asWrappedType, SLong),
            R6 -> Constant(feePerToken.asWrappedType, SLong),
            R7 -> Constant(ask.base.meta.boxId.toSigma.asWrappedType, SCollection(SByte))
          ).asInstanceOf[Map[NonMandatoryRegisterId, EvaluatedValue[SType]]]
          val residualBox =
            new ErgoBoxCandidate(rem, residualScript, ctx.currentHeight, residualTokens, residualRegisters)
          executeAsks(tl)(toFill = 0L, residualBox +: returnBox +: dexFeeBox +: acc)
        case Nil =>
          acc
      }

    @tailrec private def executeBids(
      bids: List[FilledBid]
    )(toFill: Long, acc: List[ErgoBoxCandidate] = List.empty): List[ErgoBoxCandidate] =
      bids match {
        case bid :: tl if bid.base.amount <= toFill =>
          val amount       = bid.base.amount
          val fee          = amount * bid.base.feePerToken
          val rem          = bid.base.meta.boxValue - fee - amount * bid.executionPrice
          val reward       = amount
          val pk           = bid.base.meta.pk
          val returnTokens = Colls.fromItems(bid.base.quoteAsset.toErgo -> reward)
          val returnRegisters = Map(
            R4 -> Constant(bid.base.meta.boxId.toSigma.asWrappedType, SCollection(SByte))
          ).asInstanceOf[Map[NonMandatoryRegisterId, EvaluatedValue[SType]]]
          val returnBox         = new ErgoBoxCandidate(rem, pk, ctx.currentHeight, returnTokens, returnRegisters)
          val minReturnBoxValue = returnBox.bytesWithNoRef.length * ctx.nanoErgsPerByte
          val toAdd             = minReturnBoxValue - rem
          val dexFeeLeft        = fee - toAdd
          val returnBoxRefilled =
            new ErgoBoxCandidate(minReturnBoxValue, pk, ctx.currentHeight, returnTokens, returnRegisters)
          val dexFeeBox = new ErgoBoxCandidate(dexFeeLeft, dexRewardProp, ctx.currentHeight)
          executeBids(tl)(toFill - amount, returnBoxRefilled +: dexFeeBox +: acc)
        case bid :: tl =>
          val amount       = toFill
          val price        = bid.executionPrice
          val feePerToken  = bid.base.feePerToken
          val assetId      = bid.base.quoteAsset
          val fee          = amount * feePerToken
          val rem          = bid.base.meta.boxValue - fee - amount * price
          val reward       = amount
          val pk           = bid.base.meta.pk
          val dexFeeBox    = new ErgoBoxCandidate(fee, dexRewardProp, ctx.currentHeight)
          val returnTokens = Colls.fromItems(assetId.toErgo -> reward)
          val returnRegisters = Map(
            R4 -> Constant(bid.base.meta.boxId.toSigma.asWrappedType, SCollection(SByte))
          ).asInstanceOf[Map[NonMandatoryRegisterId, EvaluatedValue[SType]]]
          val returnBox = new ErgoBoxCandidate(rem, pk, ctx.currentHeight, returnTokens, returnRegisters)
          val residualParams =
            DexBuyerContractParameters(
              bid.base.meta.pk,
              assetId.toErgo,
              bid.base.price,
              feePerToken
            )
          val residualScript = DexLimitOrderContracts.buyerContractInstance(residualParams).ergoTree
          val residualRegisters = Map(
            R4 -> Constant(assetId.toSigma.asWrappedType, SCollection(SByte)),
            R5 -> Constant(price.asWrappedType, SLong),
            R6 -> Constant(feePerToken.asWrappedType, SLong),
            R7 -> Constant(bid.base.meta.boxId.toSigma.asWrappedType, SCollection(SByte))
          ).asInstanceOf[Map[NonMandatoryRegisterId, EvaluatedValue[SType]]]
          val residualBox =
            new ErgoBoxCandidate(rem, residualScript, ctx.currentHeight, Colls.emptyColl, residualRegisters)
          executeBids(tl)(toFill = 0L, residualBox +: returnBox +: dexFeeBox +: acc)
        case Nil =>
          acc
      }
  }

  /** An aspect merging dex reward outputs into single one.
    */
  final class DexOutputsCompaction[F[_]: Functor](exchangeConfig: ExchangeConfig, protocolConfig: ProtocolConfig)
    extends Transactions[Mid[F, *]] {

    implicit private val addressEncoder: ErgoAddressEncoder = protocolConfig.networkType.addressEncoder
    private val dexRewardProp                               = exchangeConfig.rewardAddress.toErgoTree

    def translate(trade: AnyTrade): Mid[F, ErgoLikeTransaction] =
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
}
