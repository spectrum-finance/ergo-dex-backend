package org.ergoplatform.dex.executor.amm.interpreters

import cats.FlatMap
import org.ergoplatform.ErgoLikeTransaction
import org.ergoplatform.dex.domain.amm.CFMMOrder._
import org.ergoplatform.dex.domain.amm.CFMMOrderType.{DepositType, RedeemType, SwapType}
import org.ergoplatform.dex.domain.amm.{CFMMOrder, CFMMPool}
import org.ergoplatform.dex.protocol.amm.AMMType.{CFMMType, N2T_CFMM, T2T_CFMM}
import org.ergoplatform.dex.protocol.amm.InterpreterVersion
import org.ergoplatform.ergo.state.{Predicted, Traced}
import tofu.higherKind.{Mid, RepresentableK}
import tofu.logging.Logging
import tofu.syntax.logging._
import tofu.syntax.monadic._
import org.ergoplatform.dex.protocol.instances._

/** Interprets CFMM operations to a transaction
  */
trait CFMMInterpreter[CT <: CFMMType, V <: InterpreterVersion, F[_]] {

  def deposit(in: CFMMOrder.AnyDeposit, pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])]

  def redeem(in: CFMMOrder.AnyRedeem, pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])]

  def swap(in: CFMMOrder.AnySwap, pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])]
}

object CFMMInterpreter {

  implicit def representableK[CT <: CFMMType, V <: InterpreterVersion]: RepresentableK[CFMMInterpreter[CT, V, *[_]]] = {
    type Rep[F[_]] = CFMMInterpreter[CT, V, F]
    tofu.higherKind.derived.genRepresentableK[Rep]
  }

  def make[F[_]](implicit
    n2t: CFMMInterpreter[N2T_CFMM, InterpreterVersion.V1, F],
    t2t: CFMMInterpreter[T2T_CFMM, InterpreterVersion.V1, F],
    n2tV3: CFMMInterpreter[N2T_CFMM, InterpreterVersion.V3, F],
    t2tV3: CFMMInterpreter[T2T_CFMM, InterpreterVersion.V3, F]
  ): CFMMInterpreter[CFMMType, InterpreterVersion.Any, F] =
    new Proxy[F]

  final class Proxy[F[_]](implicit
    n2tV1: CFMMInterpreter[N2T_CFMM, InterpreterVersion.V1, F],
    t2tV1: CFMMInterpreter[T2T_CFMM, InterpreterVersion.V1, F],
    n2tV3: CFMMInterpreter[N2T_CFMM, InterpreterVersion.V3, F],
    t2tV3: CFMMInterpreter[T2T_CFMM, InterpreterVersion.V3, F]
  ) extends CFMMInterpreter[CFMMType, InterpreterVersion.Any, F] {

    def deposit(in: CFMMOrder[DepositType], pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])] =
      in match {
        case _: DepositErgFee   => if (pool.isNative) n2tV1.deposit(in, pool) else n2tV1.deposit(in, pool)
        case _: DepositTokenFee => if (pool.isNative) n2tV3.deposit(in, pool) else t2tV3.deposit(in, pool)
      }

    def redeem(in: CFMMOrder[RedeemType], pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])] =
      in match {
        case _: RedeemErgFee   => if (pool.isNative) n2tV1.redeem(in, pool) else t2tV1.redeem(in, pool)
        case _: RedeemTokenFee => if (pool.isNative) n2tV3.redeem(in, pool) else t2tV3.redeem(in, pool)
      }

    def swap(in: CFMMOrder[SwapType], pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])] =
      in match {
        case _: SwapP2Pk         => if (pool.isNative) n2tV1.swap(in, pool) else t2tV1.swap(in, pool)
        case _: SwapMultiAddress => if (pool.isNative) n2tV1.swap(in, pool) else t2tV1.swap(in, pool)
        case _: SwapTokenFee     => if (pool.isNative) n2tV3.swap(in, pool) else t2tV3.swap(in, pool)
      }

  }

  final class CFMMInterpreterTracing[CT <: CFMMType, V <: InterpreterVersion, F[_]: FlatMap: Logging]
    extends CFMMInterpreter[CT, V, Mid[F, *]] {

    def deposit(
      in: CFMMOrder[DepositType],
      pool: CFMMPool
    ): Mid[F, (ErgoLikeTransaction, Traced[Predicted[CFMMPool]])] =
      _ >>= (r => trace"deposit(in=$in, pool=$pool) = (${r._1}, ${r._2})" as r)

    def redeem(in: CFMMOrder[RedeemType], pool: CFMMPool): Mid[F, (ErgoLikeTransaction, Traced[Predicted[CFMMPool]])] =
      _ >>= (r => trace"redeem(in=$in, pool=$pool) = (${r._1}, ${r._2})" as r)

    def swap(in: CFMMOrder[SwapType], pool: CFMMPool): Mid[F, (ErgoLikeTransaction, Traced[Predicted[CFMMPool]])] =
      _ >>= (r => trace"swap(in=$in, pool=$pool) = (${r._1}, ${r._2})" as r)
  }
}
