package org.ergoplatform.dex.executor.amm.interpreters.v3.n2t

import cats.{Functor, Monad}
import cats.effect.concurrent.Ref
import org.ergoplatform.{ErgoAddressEncoder, ErgoLikeTransaction}
import org.ergoplatform.dex.configs.MonetaryConfig
import org.ergoplatform.dex.domain.NetworkContext
import org.ergoplatform.dex.domain.amm.CFMMOrder.{AnyDeposit, AnyRedeem, AnySwap}
import org.ergoplatform.dex.domain.amm.{CFMMOrder, CFMMPool}
import org.ergoplatform.dex.executor.amm.config.ExchangeConfig
import org.ergoplatform.dex.executor.amm.domain.errors.ExecutionFailed
import org.ergoplatform.dex.executor.amm.interpreters.CFMMInterpreter.CFMMInterpreterTracing
import org.ergoplatform.dex.executor.amm.interpreters.{CFMMInterpreter, CFMMInterpreterHelpers}
import org.ergoplatform.dex.protocol.amm.{AMMContracts, InterpreterVersion}
import org.ergoplatform.dex.protocol.amm.AMMType.N2T_CFMM
import org.ergoplatform.ergo.state.{Predicted, Traced}
import tofu.logging.Logs
import tofu.syntax.monadic._

object N2TV3 {

  def make[I[_]: Functor, F[_]: Monad: ExecutionFailed.Raise](
    exchange: ExchangeConfig,
    monetary: MonetaryConfig,
    ref: Ref[F, NetworkContext]
  )(implicit
    contracts: AMMContracts[N2T_CFMM],
    encoder: ErgoAddressEncoder,
    logs: Logs[I, F]
  ): I[CFMMInterpreter[N2T_CFMM, InterpreterVersion.V3, F]] =
    logs.forService[CFMMInterpreter[N2T_CFMM, InterpreterVersion.V3, F]].map { implicit l =>
      val helpers = new CFMMInterpreterHelpers(exchange, monetary)
      val depositI =
        new N2TDepositTokenFeeInterpreter[F](exchange: ExchangeConfig, monetary: MonetaryConfig, ref, helpers)
      val redeemI =
        new N2TRedeemTokenFeeInterpreter[F](exchange: ExchangeConfig, monetary: MonetaryConfig, ref, helpers)
      val swapI = new N2TSwapTokenFeeInterpreter[F](exchange: ExchangeConfig, monetary: MonetaryConfig, ref, helpers)
      new CFMMInterpreterTracing[N2T_CFMM, InterpreterVersion.V3, F] attach new CFMMInterpreter[
        N2T_CFMM,
        InterpreterVersion.V3,
        F
      ] {
        def deposit(in: AnyDeposit, pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])] =
          in match {
            case d: CFMMOrder.DepositTokenFee => depositI.deposit(d, pool, ???)
          }

        def redeem(in: AnyRedeem, pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])] =
          in match {
            case d: CFMMOrder.RedeemTokenFee => redeemI.redeem(d, pool, ???)
          }

        def swap(in: AnySwap, pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])] =
          in match {
            case d: CFMMOrder.SwapTokenFee => swapI.swap(d, pool, ???)
          }
      }
    }

}
