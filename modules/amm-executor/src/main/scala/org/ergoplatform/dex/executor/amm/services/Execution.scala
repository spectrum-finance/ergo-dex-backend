package org.ergoplatform.dex.executor.amm.services

import cats.syntax.option._
import cats.{Functor, Monad}
import org.ergoplatform.dex.domain.amm.{CFMMOrder, CFMMOrderType}
import org.ergoplatform.dex.domain.amm.CFMMOrder._
import org.ergoplatform.dex.domain.errors.TxFailed
import org.ergoplatform.dex.executor.amm.domain.errors.{ExecutionFailed, IncorrectMultiAddressTree}
import org.ergoplatform.dex.executor.amm.interpreters.CFMMInterpreter
import org.ergoplatform.dex.executor.amm.repositories.CFMMPools
import org.ergoplatform.dex.protocol.amm.AMMType.CFMMType
import org.ergoplatform.ergo.modules.ErgoNetwork
import org.ergoplatform.ergo.services.explorer.TxSubmissionErrorParser
import tofu.logging.{Logging, Logs}
import tofu.syntax.handle._
import tofu.syntax.logging._
import tofu.syntax.monadic._

trait Execution[F[_]] {

  /** Try to execute a given order if possible.
    * @return `None` in case the order is executed, `Some(order)` otherwise.
    */
  def executeAttempt(op: CFMMOrder.AnyOrder): F[Option[CFMMOrder.AnyOrder]]
}

object Execution {

  def make[I[_]: Functor, F[_]: Monad: TxFailed.Handle: ExecutionFailed.Handle](implicit
    pools: CFMMPools[F],
    interpreter: CFMMInterpreter[CFMMType, F],
    network: ErgoNetwork[F],
    logs: Logs[I, F]
  ): I[Execution[F]] =
    logs.forService[Execution[F]].map(implicit l => new Live[F])

  final class Live[F[_]: Monad: TxFailed.Handle: ExecutionFailed.Handle: Logging](implicit
    pools: CFMMPools[F],
    interpreter: CFMMInterpreter[CFMMType, F],
    network: ErgoNetwork[F],
    errParser: TxSubmissionErrorParser
  ) extends Execution[F] {

    def executeAttempt(order: CFMMOrder.AnyOrder): F[Option[CFMMOrder.AnyOrder]] =
      pools.get(order.poolId) >>= {
        case Some(pool) =>
          val interpretF = {
            (order, order.orderType) match {
              case (swap: CFMMOrder.AnySwap, _: CFMMOrderType.SwapType)          => interpreter.swap(swap, pool)
              case (redeem: CFMMOrder.AnyRedeem, _: CFMMOrderType.RedeemType)    => interpreter.redeem(redeem, pool)
              case (deposit: CFMMOrder.AnyDeposit, _: CFMMOrderType.DepositType) => interpreter.deposit(deposit, pool)
            }
          }
          val executeF =
            for {
              (transaction, nextPool) <- interpretF
              finalizeF = network.submitTransaction(transaction) >> pools.put(nextPool)
              res <- (finalizeF as none[CFMMOrder.AnyOrder])
                       .handleWith[TxFailed] { e =>
                         network.checkTransaction(transaction).flatMap {
                           case Some(errText) =>
                             val invalidInputs = errParser.missedInputs(errText)
                             val poolBoxId     = pool.box.boxId
                             val invalidPool   = invalidInputs.exists { case (boxId, _) => boxId == poolBoxId }
                             if (invalidPool)
                               warnCause"PoolState{poolId=${pool.poolId}, boxId=$poolBoxId} is invalidated. Validation result: $errText" (
                                 e
                               ) >>
                               pools.invalidate(pool.poolId, poolBoxId) as Some(order)
                             else
                               warnCause"Order{id=${order.id}} is discarded due to TX error. Validation result: $errText" (
                                 e
                               ) as none
                           case _ =>
                             warnCause"Order{id=${order.id}} is discarded due to TX error." (e) as none
                         }
                       }
            } yield res

          executeF.handleWith[ExecutionFailed] {
            case e: IncorrectMultiAddressTree => warnCause"Order execution failed" (e) as none
            case e                            => warnCause"Order execution failed" (e) as Some(order)
          }
        case None =>
          warn"Order{id=${order.id}} references an unknown Pool{id=${order.poolId}}" as Some(order)
      }
  }
}
