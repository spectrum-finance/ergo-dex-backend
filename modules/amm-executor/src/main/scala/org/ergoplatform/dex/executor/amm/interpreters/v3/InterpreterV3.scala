package org.ergoplatform.dex.executor.amm.interpreters.v3

import org.ergoplatform.ErgoLikeTransaction
import org.ergoplatform.dex.domain.amm.CFMMOrder.{DepositTokenFee, RedeemTokenFee, SwapTokenFee}
import org.ergoplatform.dex.domain.amm.CFMMPool
import org.ergoplatform.dex.protocol.amm.AMMType.CFMMType
import org.ergoplatform.ergo.state.{Predicted, Traced}

trait InterpreterV3[CT <: CFMMType, F[_]] {

  def deposit(deposit: DepositTokenFee, pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])]

  def redeem(redeem: RedeemTokenFee, pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])]

  def swap(swap: SwapTokenFee, pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])]
}
