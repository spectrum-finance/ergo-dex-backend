package org.ergoplatform.dex.executor.amm.interpreters

import org.ergoplatform.ErgoLikeTransaction
import org.ergoplatform.dex.domain.amm.state.Predicted
import org.ergoplatform.dex.domain.amm.{CFMMPool, Deposit, Redeem, Swap}
import org.ergoplatform.dex.protocol.amm.AMMType.CFMMFamily
import tofu.higherKind.Embed

/** Interprets CFMM operations to a transaction
  */
trait CFMMInterpreter[CT <: CFMMFamily, F[_]] {

  def deposit(in: Deposit, pool: CFMMPool): F[(ErgoLikeTransaction, Predicted[CFMMPool])]

  def redeem(in: Redeem, pool: CFMMPool): F[(ErgoLikeTransaction, Predicted[CFMMPool])]

  def swap(in: Swap, pool: CFMMPool): F[(ErgoLikeTransaction, Predicted[CFMMPool])]
}

object CFMMInterpreter {

  implicit def embed[CT <: CFMMFamily]: Embed[CFMMInterpreter[CT, *[_]]] = {
    type Rep[F[_]] = CFMMInterpreter[CT, F]
    tofu.higherKind.derived.genEmbed[Rep]
  }
}
