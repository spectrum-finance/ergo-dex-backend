package org.ergoplatform.dex.executor.amm.interpreters

import org.ergoplatform.ErgoLikeTransaction
import org.ergoplatform.dex.domain.amm.CFMMOrder._
import org.ergoplatform.dex.domain.amm.CFMMOrderType.{DepositType, RedeemType, SwapType}
import org.ergoplatform.dex.domain.amm.{CFMMOrder, CFMMPool}
import org.ergoplatform.dex.executor.amm.interpreters.v1.InterpreterV1
import org.ergoplatform.dex.executor.amm.interpreters.v3.InterpreterV3
import org.ergoplatform.dex.protocol.amm.AMMType.{CFMMType, N2T_CFMM, T2T_CFMM}
import org.ergoplatform.ergo.state.{Predicted, Traced}
import tofu.higherKind.RepresentableK

/** Interprets CFMM operations to a transaction
  */
trait CFMMInterpreter[CT <: CFMMType, F[_]] {

  def deposit(in: CFMMOrder.AnyDeposit, pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])]

  def redeem(in: CFMMOrder.AnyRedeem, pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])]

  def swap(in: CFMMOrder.AnySwap, pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])]
}

object CFMMInterpreter {

  implicit def representableK[CT <: CFMMType]: RepresentableK[CFMMInterpreter[CT, *[_]]] = {
    type Rep[F[_]] = CFMMInterpreter[CT, F]
    tofu.higherKind.derived.genRepresentableK[Rep]
  }

  def make[F[_]](implicit
    n2t: InterpreterV1[N2T_CFMM, F],
    t2t: InterpreterV1[T2T_CFMM, F],
    n2tV3: InterpreterV3[N2T_CFMM, F],
    t2tV3: InterpreterV3[T2T_CFMM, F]
  ): CFMMInterpreter[CFMMType, F] =
    new Proxy[F]

  final class Proxy[F[_]](implicit
    n2tV1: InterpreterV1[N2T_CFMM, F],
    t2tV1: InterpreterV1[T2T_CFMM, F],
    n2tV3: InterpreterV3[N2T_CFMM, F],
    t2tV3: InterpreterV3[T2T_CFMM, F]
  ) extends CFMMInterpreter[CFMMType, F] {

    def deposit(in: CFMMOrder[DepositType], pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])] =
      in match {
        case d: DepositErgFee   => if (pool.isNative) n2tV1.deposit(d, pool) else n2tV1.deposit(d, pool)
        case d: DepositTokenFee => if (pool.isNative) n2tV3.deposit(d, pool) else t2tV3.deposit(d, pool)
      }

    def redeem(in: CFMMOrder[RedeemType], pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])] =
      in match {
        case r: RedeemErgFee   => if (pool.isNative) n2tV1.redeem(r, pool) else t2tV1.redeem(r, pool)
        case r: RedeemTokenFee => if (pool.isNative) n2tV3.redeem(r, pool) else t2tV3.redeem(r, pool)
      }

    def swap(in: CFMMOrder[SwapType], pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])] =
      in match {
        case s: SwapErg      => if (pool.isNative) n2tV1.swap(s, pool) else t2tV1.swap(s, pool)
        case s: SwapTokenFee => if (pool.isNative) n2tV3.swap(s, pool) else t2tV3.swap(s, pool)
      }

  }
}
