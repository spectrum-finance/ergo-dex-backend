package org.ergoplatform.dex.executor.amm.interpreters

import org.ergoplatform.ErgoLikeTransaction
import org.ergoplatform.dex.domain.amm.state.Predicted
import org.ergoplatform.dex.domain.amm.{CFMMPool, Deposit, Redeem, Swap}
import org.ergoplatform.dex.protocol.amm.AMMType.CFMMFamily
import tofu.higherKind.Embed

/** Interprets CFMM operations to a transaction
  */
trait CfmmInterpreter[CT <: CFMMFamily, F[_]] {

  def deposit(in: Deposit, pool: CFMMPool): F[(ErgoLikeTransaction, Predicted[CFMMPool])]

  def redeem(in: Redeem, pool: CFMMPool): F[(ErgoLikeTransaction, Predicted[CFMMPool])]

  def swap(in: Swap, pool: CFMMPool): F[(ErgoLikeTransaction, Predicted[CFMMPool])]
}

object CfmmInterpreter {

  implicit def embed[CT <: CFMMFamily]: Embed[CfmmInterpreter[CT, *[_]]] = {
    type Rep[F[_]] = CfmmInterpreter[CT, F]
    tofu.higherKind.derived.genEmbed[Rep]
  }
}
