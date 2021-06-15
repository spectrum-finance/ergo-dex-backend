package org.ergoplatform.dex.executor.amm.services

import cats.Monad
import org.ergoplatform.dex.domain.amm.{CFMMOperationRequest, Deposit, Redeem, Swap}
import org.ergoplatform.dex.executor.amm.interpreters.CfmmInterpreter
import org.ergoplatform.dex.executor.amm.repositories.CfmmPools
import org.ergoplatform.dex.protocol.amm.AMMType.T2TCFMM
import org.ergoplatform.ergo.ErgoNetwork
import tofu.logging.Logging
import tofu.syntax.logging._
import tofu.syntax.monadic._

trait Execution[F[_]] {

  def execute(op: CFMMOperationRequest): F[Unit]
}

object Execution {

  final class Live[F[_]: Monad: Logging](implicit
                                         pools: CfmmPools[F],
                                         tokenToToken: CfmmInterpreter[T2TCFMM, F],
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
