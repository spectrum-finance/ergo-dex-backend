package org.ergoplatform.dex.executor.amm.interpreters.v3.t2t

import cats.effect.concurrent.Ref
import cats.{Functor, Monad}
import org.ergoplatform.dex.configs.MonetaryConfig
import org.ergoplatform.dex.domain.NetworkContext
import org.ergoplatform.dex.domain.amm.CFMMOrder._
import org.ergoplatform.dex.domain.amm.CFMMPool
import org.ergoplatform.dex.executor.amm.config.ExchangeConfig
import org.ergoplatform.dex.executor.amm.domain.errors.ExecutionFailed
import org.ergoplatform.dex.executor.amm.interpreters.CFMMInterpreterHelpers
import org.ergoplatform.dex.executor.amm.interpreters.v3.InterpreterV3
import org.ergoplatform.dex.protocol.amm.AMMContracts
import org.ergoplatform.dex.protocol.amm.AMMType.T2T_CFMM
import org.ergoplatform.ergo.state.{Predicted, Traced}
import org.ergoplatform.{ErgoAddressEncoder, ErgoLikeTransaction}
import tofu.logging.Logs
import tofu.syntax.monadic._

object T2TV3 {

  def make[I[_]: Functor, F[_]: Monad: ExecutionFailed.Raise](
    exchange: ExchangeConfig,
    monetary: MonetaryConfig,
    ref: Ref[F, NetworkContext]
  )(implicit
    contracts: AMMContracts[T2T_CFMM],
    encoder: ErgoAddressEncoder,
    logs: Logs[I, F]
  ): I[InterpreterV3[T2T_CFMM, F]] =
    logs.forService[InterpreterV3[T2T_CFMM, F]].map { implicit l =>
      val helpers  = new CFMMInterpreterHelpers(exchange, monetary)
      val depositI = new T2TDepositTokenFeeInterpreter[F](exchange, monetary, ref, helpers)
      val redeemI  = new T2TRedeemTokenFeeInterpreter[F](exchange, monetary, ref, helpers)
      val swapI    = new T2TSwapTokenFeeInterpreter[F](exchange, monetary, ref, helpers)

      new InterpreterV3[T2T_CFMM, F] {
        def deposit(deposit: DepositTokenFee, pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])] =
          depositI.deposit(deposit, pool, ???)

        def redeem(redeem: RedeemTokenFee, pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])] =
          redeemI.redeem(redeem, pool, ???)

        def swap(swap: SwapTokenFee, pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])] =
          swapI.swap(swap, pool, ???)
      }
    }

}
