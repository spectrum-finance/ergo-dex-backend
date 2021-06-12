package org.ergoplatform.dex.executor.amm.services

import cats.Monad
import org.ergoplatform.dex.domain.amm.{CfmmOperation, Deposit, Redeem, Swap}
import org.ergoplatform.dex.executor.amm.interpreters.CfmmInterpreter
import org.ergoplatform.dex.executor.amm.repositories.CfmmPools
import org.ergoplatform.dex.protocol.amm.AmmContractType.T2tCfmm
import org.ergoplatform.ergo.ErgoNetwork
import tofu.logging.Logging
import tofu.syntax.logging._
import tofu.syntax.monadic._

trait Execution[F[_]] {

  def execute(op: CfmmOperation): F[Unit]
}

object Execution {

  final class Live[F[_]: Monad: Logging](implicit
    pools: CfmmPools[F],
    t2tInterpreter: CfmmInterpreter[T2tCfmm, F],
    network: ErgoNetwork[F]
  ) extends Execution[F] {

    def execute(op: CfmmOperation): F[Unit] =
      pools.get(op.poolId) >>= {
        case Some(pool) =>
          val interpretF =
            op match {
              case deposit: Deposit => t2tInterpreter.deposit(deposit, pool)
              case redeem: Redeem   => t2tInterpreter.redeem(redeem, pool)
              case swap: Swap       => t2tInterpreter.swap(swap, pool)
            }
          interpretF >>= { case (transaction, nextPool) =>
            network.submitTransaction(transaction) >> pools.put(nextPool)
          }
        case None => warn"Operation request references unknown pool [poolId=${op.poolId}]"
      }
  }
}
