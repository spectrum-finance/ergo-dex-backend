package org.ergoplatform.dex.executor.amm.services

import cats.syntax.option._
import cats.{Functor, Monad}
import org.ergoplatform.dex.domain.amm.{CFMMOrder, Deposit, Redeem, Swap}
import org.ergoplatform.dex.domain.errors.TxFailed
import org.ergoplatform.dex.executor.amm.domain.errors.ExecutionFailed
import org.ergoplatform.dex.executor.amm.interpreters.CFMMInterpreter
import org.ergoplatform.dex.executor.amm.repositories.CFMMPools
import org.ergoplatform.dex.protocol.amm.AMMType.T2TCFMM
import org.ergoplatform.ergo.ErgoNetwork
import org.ergoplatform.ergo.explorer.TxSubmissionErrorParser
import tofu.logging.{Logging, Logs}
import tofu.syntax.handle._
import tofu.syntax.logging._
import tofu.syntax.monadic._

trait Execution[F[_]] {

  /** Try to execute a given order if possible.
    * @return `None` in case the order is executed, `Some(order)` otherwise.
    */
  def executeAttempt(op: CFMMOrder): F[Option[CFMMOrder]]
}

object Execution {

  def make[I[_]: Functor, F[_]: Monad: TxFailed.Handle: ExecutionFailed.Handle](implicit
    pools: CFMMPools[F],
    tokenToToken: CFMMInterpreter[T2TCFMM, F],
    network: ErgoNetwork[F],
    logs: Logs[I, F]
  ): I[Execution[F]] =
    logs.forService[Execution[F]].map(implicit l => new Live[F])

  final class Live[F[_]: Monad: TxFailed.Handle: ExecutionFailed.Handle: Logging](implicit
    pools: CFMMPools[F],
    tokenToToken: CFMMInterpreter[T2TCFMM, F],
    network: ErgoNetwork[F],
    errParser: TxSubmissionErrorParser
  ) extends Execution[F] {

    def executeAttempt(order: CFMMOrder): F[Option[CFMMOrder]] =
      pools.get(order.poolId) >>= {
        case Some(pool) =>
          val interpretF =
            order match {
              case deposit: Deposit => tokenToToken.deposit(deposit, pool)
              case redeem: Redeem   => tokenToToken.redeem(redeem, pool)
              case swap: Swap       => tokenToToken.swap(swap, pool)
            }
          val executeF =
            for {
              (transaction, nextPool) <- interpretF
              _                       <- network.submitTransaction(transaction)
              _                       <- pools.put(nextPool)
            } yield none[CFMMOrder]
          executeF
            .handleWith[ExecutionFailed](e => warnCause"Order execution failed" (e) as Option(order))
            .handleWith[TxFailed] { e =>
              val invalidInputs = errParser.missedInputs(e.reason)
              val poolBoxId     = pool.box.boxId
              val invalidPool   = invalidInputs.exists { case (boxId, _) => boxId == poolBoxId }
              if (invalidPool)
                warnCause"PoolState{id=${order.poolId}, boxId=$poolBoxId} has to be invalidated" (e) >>
                pools.invalidate(poolBoxId) as none
              else warnCause"Order{id=${order.id}} is discarded due to TX error" (e) as none
            }
        case None =>
          warn"Order{id=${order.id}} references an unknown Pool{id=${order.poolId}}" as none
      }
  }
}
