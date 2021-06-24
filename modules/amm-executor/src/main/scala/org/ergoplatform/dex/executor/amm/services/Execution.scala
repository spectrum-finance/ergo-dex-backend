package org.ergoplatform.dex.executor.amm.services

import cats.{Functor, Monad}
import org.ergoplatform.dex.domain.amm.{CFMMOperationRequest, Deposit, Redeem, Swap}
import org.ergoplatform.dex.executor.amm.interpreters.CFMMInterpreter
import org.ergoplatform.dex.executor.amm.repositories.CFMMPools
import org.ergoplatform.dex.protocol.amm.AMMType.T2TCFMM
import org.ergoplatform.ergo.ErgoNetwork
import tofu.logging.{Logging, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._

trait Execution[F[_]] {

  def execute(op: CFMMOperationRequest): F[Unit]
}

object Execution {

  def make[I[_]: Functor, F[_]: Monad](implicit
    pools: CFMMPools[F],
    tokenToToken: CFMMInterpreter[T2TCFMM, F],
    network: ErgoNetwork[F],
    logs: Logs[I, F]
  ): I[Execution[F]] =
    logs.forService[Execution[F]].map(implicit l => new Live[F])

  final class Live[F[_]: Monad: Logging](implicit
    pools: CFMMPools[F],
    tokenToToken: CFMMInterpreter[T2TCFMM, F],
    network: ErgoNetwork[F]
  ) extends Execution[F] {

    def execute(op: CFMMOperationRequest): F[Unit] =
      pools.get(op.poolId) >>= {
        case Some(pool) =>
          val interpretF =
            op match {
              case deposit: Deposit => tokenToToken.deposit(deposit, pool)
              case redeem: Redeem   => tokenToToken.redeem(redeem, pool)
              case swap: Swap       => tokenToToken.swap(swap, pool)
            }
          interpretF >>= { case (transaction, nextPool) =>
            network.submitTransaction(transaction) >> pools.put(nextPool)
          }
        case None => warn"Operation request references unknown pool [poolId=${op.poolId}]"
      }
  }
}
