package org.ergoplatform.dex.executor.services

import cats.Monad
import cats.data.NonEmptyList
import org.ergoplatform.ErgoBox.{NonMandatoryRegisterId, R4}
import org.ergoplatform.contracts.{DexBuyerContractParameters, DexLimitOrderContracts, DexSellerContractParameters}
import org.ergoplatform.dex.domain.models.Match.AnyMatch
import org.ergoplatform.dex.domain.models.Order.{AnyOrder, BuyOrder, SellOrder}
import org.ergoplatform.dex.domain.syntax.ergo._
import org.ergoplatform.dex.domain.syntax.match_._
import org.ergoplatform.dex.executor.context.TxContext
import org.ergoplatform.{ErgoBoxCandidate, ErgoLikeTransaction, Input}
import sigmastate.SType.AnyOps
import sigmastate.Values.{EvaluatedValue, _}
import sigmastate._
import sigmastate.eval._
import sigmastate.interpreter.ProverResult
import tofu.syntax.monadic._

abstract class TransactionService[F[_]] {

  /** Assembly Ergo transaction from a given `match`.
    */
  def makeTx(anyMatch: AnyMatch)(ctx: TxContext): F[ErgoLikeTransaction]
}

object TransactionService {

  /** Implements processing of trades necessarily involving ERG.
    */
  final private class ErgoToTokenTransactionService[F[_]: Monad] extends TransactionService[F] {

    def makeTx(anyMatch: AnyMatch)(implicit ctx: TxContext): F[ErgoLikeTransaction] = {
      val out = outputs(anyMatch).toList.toVector
      val in  = inputs(anyMatch).toList.toVector
      ErgoLikeTransaction(in, out).pure
    }
  }

  final case class OrderParams(
    fee: Long,
    rem: Long,
    filledAmount: Long,
    remAmount: Long,
    pk: ErgoTree
  )

  private val MinBoxValue = 1000000

  def orderParams(order: AnyOrder, canFill: Long): OrderParams = {
    val amount       = math.min(order.amount, canFill)
    val fee          = amount * order.feePerToken
    val rem          = order.meta.boxValue - fee
    val filledAmount = amount * order.price
    val pk           = order.meta.ownerAddress.script
    val remAmount    = order.amount - canFill
    val boxValue     = math.max(rem, MinBoxValue)
    val feeAmount    = if (boxValue == rem) fee else fee - MinBoxValue
    OrderParams(feeAmount, boxValue, filledAmount, remAmount, pk)
  }

  private[services] def inputs(anyMatch: AnyMatch): NonEmptyList[Input] =
    anyMatch.allOrders.map { ord =>
      Input(ord.meta.boxId.toErgo, ProverResult.empty)
    }

  private[services] def outputs(anyMatch: AnyMatch)(implicit ctx: TxContext): NonEmptyList[ErgoBoxCandidate] =
    ???

  private[services] def sellOut(order: SellOrder, canFill: Long)(implicit ctx: TxContext): NonEmptyList[ErgoBoxCandidate] = {
    val OrderParams(fee, rem, filledAmount, remAmount, pk) = orderParams(order, canFill)
    val dexFee       = new ErgoBoxCandidate(fee, ???, ctx.curHeight)
    if (remAmount <= 0) {
      val ret = new ErgoBoxCandidate(filledAmount + rem, pk, ctx.curHeight)
      NonEmptyList.of(ret, dexFee)
    } else {
      val ret = new ErgoBoxCandidate(filledAmount, pk, ctx.curHeight)
      val residualParams =
        DexSellerContractParameters(order.meta.ownerAddress.pubkey, order.asset.toErgo, order.price, order.feePerToken)
      val residualScript = DexLimitOrderContracts.sellerContractInstance(residualParams).ergoTree
      val residualTokens = Colls.fromItems(order.asset.toErgo -> remAmount)
      val residualRegisters = Map(R4 -> Constant(order.meta.boxId.toSigma.asWrappedType, SCollection(SByte)).value)
        .asInstanceOf[Map[NonMandatoryRegisterId, EvaluatedValue[SType]]]
      val residual = new ErgoBoxCandidate(rem, residualScript, ctx.curHeight, residualTokens, residualRegisters)
      NonEmptyList.of(ret, residual, dexFee)
    }
  }

  private[services] def buyOut(order: BuyOrder, canFill: Long)(implicit ctx: TxContext): NonEmptyList[ErgoBoxCandidate] = {
    val OrderParams(fee, rem, filledAmount, remAmount, pk) = orderParams(order, canFill)
    val retTokens    = Colls.fromItems(order.asset.toErgo -> filledAmount)
    val dexFee       = new ErgoBoxCandidate(fee, ???, ctx.curHeight)
    if (remAmount <= 0) {
      val ret = new ErgoBoxCandidate(rem, pk, ctx.curHeight, retTokens)
      NonEmptyList.of(ret, dexFee)
    } else {
      val ret = new ErgoBoxCandidate(rem, pk, ctx.curHeight, retTokens)
      val residualParams =
        DexBuyerContractParameters(order.meta.ownerAddress.pubkey, order.asset.toErgo, order.price, order.feePerToken)
      val residualScript = DexLimitOrderContracts.buyerContractInstance(residualParams).ergoTree
      val residualRegisters = Map(R4 -> Constant(order.meta.boxId.toSigma.asWrappedType, SCollection(SByte)).value)
        .asInstanceOf[Map[NonMandatoryRegisterId, EvaluatedValue[SType]]]
      val residual = new ErgoBoxCandidate(rem, residualScript, ctx.curHeight, Colls.emptyColl, residualRegisters)
      NonEmptyList.of(ret, residual, dexFee)
    }
  }
}
