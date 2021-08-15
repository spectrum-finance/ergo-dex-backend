package org.ergoplatform.dex.executor.amm.interpreters

import cats.FlatMap
import org.ergoplatform.ErgoLikeTransaction
import org.ergoplatform.dex.domain.amm.state.Predicted
import org.ergoplatform.dex.domain.amm.{CFMMPool, Deposit, Redeem, Swap}
import org.ergoplatform.dex.protocol.amm.AMMType.CFMMFamily
import org.ergoplatform.dex.protocol.instances._
import tofu.higherKind.{Mid, RepresentableK}
import tofu.logging.Logging
import tofu.syntax.logging._
import tofu.syntax.monadic._

/** Interprets CFMM operations to a transaction
  */
trait CFMMInterpreter[CT <: CFMMFamily, F[_]] {

  def deposit(in: Deposit, pool: CFMMPool): F[(ErgoLikeTransaction, Predicted[CFMMPool])]

  def redeem(in: Redeem, pool: CFMMPool): F[(ErgoLikeTransaction, Predicted[CFMMPool])]

  def swap(in: Swap, pool: CFMMPool): F[(ErgoLikeTransaction, Predicted[CFMMPool])]
}

object CFMMInterpreter {

  implicit def representableK[CT <: CFMMFamily]: RepresentableK[CFMMInterpreter[CT, *[_]]] = {
    type Rep[F[_]] = CFMMInterpreter[CT, F]
    tofu.higherKind.derived.genRepresentableK[Rep]
  }

  final class CFMMInterpreterTracing[CT <: CFMMFamily, F[_]: FlatMap: Logging] extends CFMMInterpreter[CT, Mid[F, *]] {

    def deposit(in: Deposit, pool: CFMMPool): Mid[F, (ErgoLikeTransaction, Predicted[CFMMPool])] =
      _ >>= (r => trace"deposit(in=$in, pool=$pool) = (${r._1}, ${r._2})" as r)

    def redeem(in: Redeem, pool: CFMMPool): Mid[F, (ErgoLikeTransaction, Predicted[CFMMPool])] =
      _ >>= (r => trace"redeem(in=$in, pool=$pool) = (${r._1}, ${r._2})" as r)

    def swap(in: Swap, pool: CFMMPool): Mid[F, (ErgoLikeTransaction, Predicted[CFMMPool])] =
      _ >>= (r => trace"swap(in=$in, pool=$pool) = (${r._1}, ${r._2})" as r)
  }
}
