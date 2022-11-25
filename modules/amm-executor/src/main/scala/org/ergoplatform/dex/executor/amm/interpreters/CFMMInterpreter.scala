package org.ergoplatform.dex.executor.amm.interpreters

import cats.FlatMap
import org.ergoplatform.ErgoLikeTransaction
import org.ergoplatform.ergo.state.{Predicted, Traced}
import org.ergoplatform.dex.domain.amm.CFMMPool
import org.ergoplatform.dex.domain.amm.CFMMOrder._
import org.ergoplatform.dex.protocol.amm.AMMType.{CFMMType, N2T_CFMM, T2T_CFMM}
import org.ergoplatform.dex.protocol.instances._
import tofu.higherKind.{Mid, RepresentableK}
import tofu.logging.Logging
import tofu.syntax.logging._
import tofu.syntax.monadic._

/** Interprets CFMM operations to a transaction
  */
trait CFMMInterpreter[CT <: CFMMType, F[_]] {

  def deposit(in: DepositAny, pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])]

  def redeem(in: RedeemErgFee, pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])]

  def swap(in: SwapAny, pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])]
}

object CFMMInterpreter {

  implicit def representableK[CT <: CFMMType]: RepresentableK[CFMMInterpreter[CT, *[_]]] = {
    type Rep[F[_]] = CFMMInterpreter[CT, F]
    tofu.higherKind.derived.genRepresentableK[Rep]
  }

  def make[F[_]](implicit
    n2t: CFMMInterpreter[N2T_CFMM, F],
    t2t: CFMMInterpreter[T2T_CFMM, F]
  ): CFMMInterpreter[CFMMType, F] =
    new Proxy[F]

  final class Proxy[F[_]](implicit n2t: CFMMInterpreter[N2T_CFMM, F], t2t: CFMMInterpreter[T2T_CFMM, F])
    extends CFMMInterpreter[CFMMType, F] {

    def deposit(in: DepositAny, pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])] = {
      in match {
        case native: DepositErgFee                =>
          if (pool.isNative) n2t.deposit(in, pool)
          else t2t.deposit(in, pool)
        case token: DepositTokenFee => ???
      }
    }

    def redeem(in: RedeemErgFee, pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])] =
      if (pool.isNative) n2t.redeem(in, pool)
      else t2t.redeem(in, pool)

    def swap(in: SwapAny, pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])] =
      if (pool.isNative) n2t.swap(in, pool)
      else t2t.swap(in, pool)
  }

  final class CFMMInterpreterTracing[CT <: CFMMType, F[_]: FlatMap: Logging] extends CFMMInterpreter[CT, Mid[F, *]] {

    def deposit(in: DepositAny, pool: CFMMPool): Mid[F, (ErgoLikeTransaction, Traced[Predicted[CFMMPool]])] =
      _ >>= (r => trace"deposit(in=$in, pool=$pool) = (${r._1}, ${r._2})" as r)

    def redeem(in: RedeemErgFee, pool: CFMMPool): Mid[F, (ErgoLikeTransaction, Traced[Predicted[CFMMPool]])] =
      _ >>= (r => trace"redeem(in=$in, pool=$pool) = (${r._1}, ${r._2})" as r)

    def swap(in: SwapAny, pool: CFMMPool): Mid[F, (ErgoLikeTransaction, Traced[Predicted[CFMMPool]])] =
      _ >>= (r => trace"swap(in=$in, pool=$pool) = (${r._1}, ${r._2})" as r)
  }
}
