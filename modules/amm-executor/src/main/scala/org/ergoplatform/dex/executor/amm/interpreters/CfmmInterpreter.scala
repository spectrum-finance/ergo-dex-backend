package org.ergoplatform.dex.executor.amm.interpreters

import org.ergoplatform.ErgoLikeTransaction
import org.ergoplatform.dex.domain.amm.{Deposit, Redeem, Swap}
import org.ergoplatform.dex.protocol.amm.AmmContractType.CfmmFamily
import tofu.higherKind.Embed

/** Interprets CFMM operations to a transaction
  */
trait CfmmInterpreter[CT <: CfmmFamily, F[_]] {

  def deposit(in: Deposit): F[ErgoLikeTransaction]

  def redeem(in: Redeem): F[ErgoLikeTransaction]

  def swap(in: Swap): F[ErgoLikeTransaction]
}

object CfmmInterpreter {

  implicit def embed[CT <: CfmmFamily]: Embed[CfmmInterpreter[CT, *[_]]] = {
    type Rep[F[_]] = CfmmInterpreter[CT, F]
    tofu.higherKind.derived.genEmbed[Rep]
  }
}
