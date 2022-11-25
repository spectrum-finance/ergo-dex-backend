package org.ergoplatform.dex.executor.amm.interpreters.v3

import cats.FlatMap
import derevo.derive
import org.ergoplatform.ErgoLikeTransaction
import org.ergoplatform.dex.domain.amm.CFMMOrder.{DepositTokenFee, RedeemTokenFee, SwapTokenFee}
import org.ergoplatform.dex.domain.amm.CFMMPool
import org.ergoplatform.dex.protocol.amm.AMMType.CFMMType
import org.ergoplatform.ergo.domain.Output
import org.ergoplatform.ergo.state.{Predicted, Traced}
import tofu.higherKind.Mid
import tofu.logging.Logging
import tofu.syntax.monadic._
import tofu.syntax.logging._
import org.ergoplatform.dex.protocol.instances._
import tofu.higherKind.derived.representableK

@derive(representableK)
trait InterpreterV3[CT <: CFMMType, F[_]] {

  def deposit(deposit: DepositTokenFee, pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]], Output)]

  def redeem(redeem: RedeemTokenFee, pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]], Output)]

  def swap(swap: SwapTokenFee, pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]], Output)]
}

object InterpreterV3 {

  final class InterpreterV3Tracing[CT <: CFMMType, F[_]: FlatMap: Logging] extends InterpreterV3[CT, Mid[F, *]] {

    def deposit(
      deposit: DepositTokenFee,
      pool: CFMMPool
    ): Mid[F, (ErgoLikeTransaction, Traced[Predicted[CFMMPool]], Output)] =
      _ >>= (r => trace"deposit(in=$deposit, pool=$pool) = (${r._1}, ${r._2})" as r)

    def redeem(
      redeem: RedeemTokenFee,
      pool: CFMMPool
    ): Mid[F, (ErgoLikeTransaction, Traced[Predicted[CFMMPool]], Output)] =
      _ >>= (r => trace"redeem(in=$redeem, pool=$pool) = (${r._1}, ${r._2})" as r)

    def swap(swap: SwapTokenFee, pool: CFMMPool): Mid[F, (ErgoLikeTransaction, Traced[Predicted[CFMMPool]], Output)] =
      _ >>= (r => trace"swap(in=$swap, pool=$pool) = (${r._1}, ${r._2})" as r)
  }
}
