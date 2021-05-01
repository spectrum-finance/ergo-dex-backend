package org.ergoplatform.dex.executor.orders.interpreters

import org.ergoplatform.ErgoLikeTransaction
import org.ergoplatform.dex.clients.ErgoNetwork
import org.ergoplatform.dex.domain.amm.{Deposit, Redeem, Swap}
import org.ergoplatform.dex.executor.orders.domain.NetworkContext
import org.ergoplatform.dex.executor.orders.repositories.Pools
import org.ergoplatform.dex.protocol.amm.AmmContractType.T2tCfmm

final class T2tCfmmInterpreter[F[_]](networkContext: NetworkContext)(implicit pools: Pools[F])
  extends CfmmInterpreter[T2tCfmm, F] {

  def deposit(op: Deposit): F[ErgoLikeTransaction] = ???

  def redeem(op: Redeem): F[ErgoLikeTransaction] = ???

  def swap(op: Swap): F[ErgoLikeTransaction] = ???
    //pools.get(op.poolId)
}
