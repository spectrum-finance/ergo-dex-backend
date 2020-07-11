package org.ergoplatform.dex.executor.services

import cats.Monad
import cats.data.NonEmptyList
import org.ergoplatform.ErgoBox.{NonMandatoryRegisterId, R4}
import org.ergoplatform.contracts.{DexLimitOrderContracts, DexSellerContractParameters}
import org.ergoplatform.dex.domain.models.Match.AnyMatch
import org.ergoplatform.dex.domain.models.{Order, OrderType}
import org.ergoplatform.dex.domain.syntax.ergo._
import org.ergoplatform.dex.domain.syntax.match_._
import org.ergoplatform.dex.executor.context.TxContext
import org.ergoplatform.{ErgoBoxCandidate, ErgoLikeTransaction, Input}
import sigmastate.interpreter.ProverResult
import tofu.syntax.monadic._
import sigmastate.SType.AnyOps
import sigmastate.Values.{EvaluatedValue, _}
import sigmastate._
import sigmastate.eval._

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

  private[services] def inputs(anyMatch: AnyMatch): NonEmptyList[Input] =
    anyMatch.allOrders.map { ord =>
      Input(ord.meta.boxId.toErgo, ProverResult.empty)
    }

  private[services] def outputs(anyMatch: AnyMatch)(implicit ctx: TxContext): NonEmptyList[ErgoBoxCandidate] = ???

  private def sellOut[T <: OrderType](order: Order[T], canFill: Long)(implicit
    ctx: TxContext
  ): NonEmptyList[ErgoBoxCandidate] = {
    val fee         = order.amount * order.feePerToken
    val rem         = order.meta.boxValue - fee
    val filledValue = order.amount * order.price
    val pk          = order.meta.ownerAddress.script
    val remAmount   = order.amount - canFill
    if (remAmount <= 0) {
      val ret = new ErgoBoxCandidate(filledValue + rem, pk, ctx.curHeight)
      NonEmptyList.one(ret)
    } else {
      val ret = new ErgoBoxCandidate(filledValue, pk, ctx.curHeight)
      val residualParams =
        DexSellerContractParameters(order.meta.ownerAddress.pubkey, order.asset.toErgo, order.price, order.feePerToken)
      val residualScript = DexLimitOrderContracts.sellerContractInstance(residualParams).ergoTree
      val residualTokens = Colls.fromItems(order.asset.toErgo -> remAmount)
      val residualRegisters = Map(R4 -> Constant(order.meta.boxId.toSigma.asWrappedType, SCollection(SByte)).value)
        .asInstanceOf[Map[NonMandatoryRegisterId, EvaluatedValue[SType]]]
      val residual = new ErgoBoxCandidate(rem, residualScript, ctx.curHeight, residualTokens, residualRegisters)
      NonEmptyList.of(ret, residual)
    }
  }
}
