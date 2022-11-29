package org.ergoplatform.dex.executor.amm.interpreters.v1

import cats.FlatMap
import derevo.derive
import org.ergoplatform.ErgoLikeTransaction
import org.ergoplatform.dex.domain.amm.CFMMOrder.{DepositErgFee, RedeemErgFee, SwapErg}
import org.ergoplatform.dex.domain.amm.CFMMPool
import org.ergoplatform.dex.protocol.amm.AMMType.CFMMType
import org.ergoplatform.ergo.state.{Predicted, Traced}
import tofu.higherKind.Mid
import tofu.higherKind.derived.representableK
import tofu.logging.Logging
import tofu.syntax.monadic._
import tofu.syntax.logging._
import org.ergoplatform.dex.protocol.instances._

@derive(representableK)
trait InterpreterV1[CT <: CFMMType, F[_]] {

  def deposit(deposit: DepositErgFee, pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])]

  def redeem(redeem: RedeemErgFee, pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])]

  def swap(swap: SwapErg, pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])]

}

object InterpreterV1 {

  final class InterpreterV1Tracing[CT <: CFMMType, F[_]: FlatMap: Logging] extends InterpreterV1[CT, Mid[F, *]] {

    def deposit(deposit: DepositErgFee, pool: CFMMPool): Mid[F, (ErgoLikeTransaction, Traced[Predicted[CFMMPool]])] =
      _ >>= (r => trace"deposit(in=$deposit, pool=$pool) = (${r._1}, ${r._2})" as r)

    def redeem(redeem: RedeemErgFee, pool: CFMMPool): Mid[F, (ErgoLikeTransaction, Traced[Predicted[CFMMPool]])] =
      _ >>= (r => trace"redeem(in=$redeem, pool=$pool) = (${r._1}, ${r._2})" as r)

    def swap(swap: SwapErg, pool: CFMMPool): Mid[F, (ErgoLikeTransaction, Traced[Predicted[CFMMPool]])] =
      _ >>= (r => trace"swap(in=$swap, pool=$pool) = (${r._1}, ${r._2})" as r)
  }
}
