package org.ergoplatform.dex.executor.modules

import cats.data.NonEmptyList
import cats.syntax.option._
import cats.{Applicative, Functor, Monad}
import derevo.derive
import org.ergoplatform.ErgoBox.{NonMandatoryRegisterId, R4}
import org.ergoplatform.contracts.{DexBuyerContractParameters, DexLimitOrderContracts, DexSellerContractParameters}
import org.ergoplatform.dex.configs.ProtocolConfig
import org.ergoplatform.dex.domain.models.Order._
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
import sigmastate.{SByte, SCollection, SType}
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

    private def inputs(orders: NonEmptyList[AnyOrder]): NonEmptyList[Input] =
      orders.map(ord => Input(ord.meta.boxId.toErgo, ProverResult.empty))

    private def outputs(asks: NonEmptyList[Ask], bids: NonEmptyList[Bid]): F[NonEmptyList[ErgoBoxCandidate]] = {
      val askAmount = asks.toList.map(_.amount).sum
      val bidAmount = bids.toList.map(_.amount).sum
      val outputs   = executeAsks(asks.toList)(bidAmount) ++ executeBids(bids.toList)(askAmount)
      NonEmptyList.fromListUnsafe(outputs).pure
    }

    @tailrec private def executeAsks(
      asks: List[Ask]
    )(toFill: Long, acc: List[ErgoBoxCandidate] = List.empty): List[ErgoBoxCandidate] =
      asks match {
        case ask :: tl if ask.amount <= toFill =>
          val amount    = ask.amount
          val fee       = amount * ask.feePerToken
          val rem       = ask.meta.boxValue - fee // todo: what if remainder is too small?
          val reward    = amount * ask.price
          val pk        = ask.meta.pk
          val dexFeeBox = new ErgoBoxCandidate(fee, dexRewardProp, ctx.curHeight)
          val rewardBox = new ErgoBoxCandidate(reward + rem, pk, ctx.curHeight)
          executeAsks(tl)(toFill - amount, rewardBox +: dexFeeBox +: acc)
        case ask :: tl =>
          val amount    = toFill
          val fee       = amount * ask.feePerToken
          val rem       = ask.meta.boxValue - fee // todo: what if remainder is too small?
          val reward    = amount * ask.price
          val pk        = ask.meta.pk
          val unfilled  = ask.amount - toFill
          val dexFeeBox = new ErgoBoxCandidate(fee, dexRewardProp, ctx.curHeight)
          val rewardBox = new ErgoBoxCandidate(reward, pk, ctx.curHeight)
          val residualParams =
            DexSellerContractParameters(
              ask.meta.pk,
              ask.quoteAsset.toErgo,
              ask.price,
              ask.feePerToken
            )
          val residualScript = DexLimitOrderContracts.sellerContractInstance(residualParams).ergoTree
          val residualTokens = Colls.fromItems(ask.quoteAsset.toErgo -> unfilled)
          val residualRegisters = Map(R4 -> Constant(ask.meta.boxId.toSigma.asWrappedType, SCollection(SByte)).value)
            .asInstanceOf[Map[NonMandatoryRegisterId, EvaluatedValue[SType]]]
          val residualBox = new ErgoBoxCandidate(rem, residualScript, ctx.curHeight, residualTokens, residualRegisters)
          executeAsks(tl)(toFill = 0L, residualBox +: rewardBox +: dexFeeBox +: acc)
        case Nil =>
          acc
      }

    @tailrec private def executeBids(
      bids: List[Bid]
    )(toFill: Long, acc: List[ErgoBoxCandidate] = List.empty): List[ErgoBoxCandidate] =
      bids match {
        case bid :: tl if bid.amount <= toFill =>
          val amount       = bid.amount
          val fee          = amount * bid.feePerToken
          val rem          = bid.meta.boxValue - fee - amount * bid.price
          val reward       = amount
          val pk           = bid.meta.pk
          val dexFeeBox    = new ErgoBoxCandidate(fee, dexRewardProp, ctx.curHeight)
          val rewardTokens = Colls.fromItems(bid.quoteAsset.toErgo -> reward)
          val rewardBox    = new ErgoBoxCandidate(rem, pk, ctx.curHeight, rewardTokens)
          executeBids(tl)(toFill - amount, rewardBox +: dexFeeBox +: acc)
        case bid :: tl =>
          val amount       = toFill
          val fee          = amount * bid.feePerToken
          val rem          = bid.meta.boxValue - fee - amount * bid.price
          val reward       = amount
          val pk           = bid.meta.pk
          val dexFeeBox    = new ErgoBoxCandidate(fee, dexRewardProp, ctx.curHeight)
          val rewardTokens = Colls.fromItems(bid.quoteAsset.toErgo -> reward)
          val rewardBox    = new ErgoBoxCandidate(rem, pk, ctx.curHeight, rewardTokens)
          val residualParams =
            DexBuyerContractParameters(
              bid.meta.pk,
              bid.quoteAsset.toErgo,
              bid.price,
              bid.feePerToken
            )
          val residualScript = DexLimitOrderContracts.buyerContractInstance(residualParams).ergoTree
          val residualRegisters = Map(R4 -> Constant(bid.meta.boxId.toSigma.asWrappedType, SCollection(SByte)).value)
            .asInstanceOf[Map[NonMandatoryRegisterId, EvaluatedValue[SType]]]
          val residualBox = new ErgoBoxCandidate(rem, residualScript, ctx.curHeight, Colls.emptyColl, residualRegisters)
          executeBids(tl)(toFill = 0L, residualBox +: rewardBox +: dexFeeBox +: acc)
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
