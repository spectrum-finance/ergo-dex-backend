package org.ergoplatform.dex.executor.amm.interpreters

import cats.{Functor, Monad}
import org.ergoplatform.ErgoLikeTransaction
import org.ergoplatform.dex.domain.amm.CFMMOrder._
import org.ergoplatform.dex.domain.amm.CFMMOrderType.{DepositType, RedeemType, SwapType}
import org.ergoplatform.dex.domain.amm.{CFMMOrder, CFMMPool}
import org.ergoplatform.dex.executor.amm.interpreters.v1.InterpreterV1
import org.ergoplatform.dex.executor.amm.interpreters.v3.InterpreterV3
import org.ergoplatform.dex.protocol.amm.AMMType.{CFMMType, N2T_CFMM, T2T_CFMM}
import org.ergoplatform.ergo.domain.Output
import org.ergoplatform.ergo.state.{Predicted, Traced}
import tofu.higherKind.{Mid, RepresentableK}
import tofu.logging.{Logging, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._

/** Interprets CFMM operations to a transaction
  */
trait CFMMInterpreter[CT <: CFMMType, F[_]] {

  def deposit(
    in: CFMMOrder.AnyDeposit,
    pool: CFMMPool
  ): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]], Output)]

  def redeem(
    in: CFMMOrder.AnyRedeem,
    pool: CFMMPool
  ): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]], Output)]

  def swap(in: CFMMOrder.AnySwap, pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]], Output)]
}

object CFMMInterpreter {

  implicit def representableK[CT <: CFMMType]: RepresentableK[CFMMInterpreter[CT, *[_]]] = {
    type Rep[F[_]] = CFMMInterpreter[CT, F]
    tofu.higherKind.derived.genRepresentableK[Rep]
  }

  def make[I[_]: Functor, F[_]: Monad](implicit
    n2t: InterpreterV1[N2T_CFMM, F],
    t2t: InterpreterV1[T2T_CFMM, F],
    n2tV3: InterpreterV3[N2T_CFMM, F],
    t2tV3: InterpreterV3[T2T_CFMM, F],
    logs: Logs[I, F]
  ): I[CFMMInterpreter[CFMMType, F]] =
    logs.forService[CFMMInterpreter[CFMMType, F]].map { implicit __ =>
      new Tracing[F] attach new Proxy[F]
    }

  final class Proxy[F[_]: Functor](implicit
    n2tV1: InterpreterV1[N2T_CFMM, F],
    t2tV1: InterpreterV1[T2T_CFMM, F],
    n2tV3: InterpreterV3[N2T_CFMM, F],
    t2tV3: InterpreterV3[T2T_CFMM, F]
  ) extends CFMMInterpreter[CFMMType, F] {

    def deposit(
      in: CFMMOrder[DepositType],
      pool: CFMMPool
    ): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]], Output)] =
      in match {
        case d: DepositErgFee =>
          if (pool.isNative) n2tV1.deposit(d, pool)
          else t2tV1.deposit(d, pool)
        case d: DepositTokenFee => if (pool.isNative) n2tV3.deposit(d, pool) else t2tV3.deposit(d, pool)
      }

    def redeem(
      in: CFMMOrder[RedeemType],
      pool: CFMMPool
    ): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]], Output)] =
      in match {
        case r: RedeemErgFee =>
          if (pool.isNative) n2tV1.redeem(r, pool)
          else t2tV1.redeem(r, pool)
        case r: RedeemTokenFee => if (pool.isNative) n2tV3.redeem(r, pool) else t2tV3.redeem(r, pool)
      }

    def swap(
      in: CFMMOrder[SwapType],
      pool: CFMMPool
    ): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]], Output)] =
      in match {
        case s: SwapErg =>
          if (pool.isNative) n2tV1.swap(s, pool)
          else t2tV1.swap(s, pool)
        case s: SwapTokenFee => if (pool.isNative) n2tV3.swap(s, pool) else t2tV3.swap(s, pool)
      }

  }

  final private class Tracing[F[_]: Monad: Logging] extends CFMMInterpreter[CFMMType, Mid[F, *]] {

    def deposit(
      in: AnyDeposit,
      pool: CFMMPool
    ): Mid[F, (ErgoLikeTransaction, Traced[Predicted[CFMMPool]], Output)] =
      for {
        _ <- info"deposit(${in.box.boxId}, ${pool.box.boxId})"
        r <- _
        _ <- info"deposit(${in.box.boxId}, ${pool.box.boxId}) -> ${s"${r._1.id}"}"
      } yield r

    def redeem(
      in: AnyRedeem,
      pool: CFMMPool
    ): Mid[F, (ErgoLikeTransaction, Traced[Predicted[CFMMPool]], Output)] =
      for {
        _ <- info"redeem(${in.box.boxId}, ${pool.box.boxId})"
        r <- _
        _ <- info"redeem(${in.box.boxId}, ${pool.box.boxId}) -> ${s"${r._1.id}"}"
      } yield r

    def swap(in: AnySwap, pool: CFMMPool): Mid[F, (ErgoLikeTransaction, Traced[Predicted[CFMMPool]], Output)] =
      for {
        _ <- info"swap(${in.box.boxId}, ${pool.box.boxId})"
        r <- _
        _ <- info"swap(${in.box.boxId}, ${pool.box.boxId}) -> ${s"${r._1.id}"}"
      } yield r
  }
}
