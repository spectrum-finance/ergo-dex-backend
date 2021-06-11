package org.ergoplatform.dex.executor.amm.interpreters

import org.ergoplatform.ErgoLikeTransaction
import org.ergoplatform.dex.domain.amm.{CfmmPool, Deposit, Redeem, Swap}
import org.ergoplatform.dex.protocol.amm.AmmContractType.CfmmFamily
import tofu.higherKind.Embed

/** Interprets CFMM operations to a transaction
  */
trait CfmmInterpreter[CT <: CfmmFamily, F[_]] {

  def deposit(in: Deposit, pool: CfmmPool): F[(ErgoLikeTransaction, CfmmPool)]

  def redeem(in: Redeem, pool: CfmmPool): F[(ErgoLikeTransaction, CfmmPool)]

  def swap(in: Swap, pool: CfmmPool): F[(ErgoLikeTransaction, CfmmPool)]
}

object CfmmInterpreter {

  implicit def embed[CT <: CfmmFamily]: Embed[CfmmInterpreter[CT, *[_]]] = {
    type Rep[F[_]] = CfmmInterpreter[CT, F]
    tofu.higherKind.derived.genEmbed[Rep]
  }
}
