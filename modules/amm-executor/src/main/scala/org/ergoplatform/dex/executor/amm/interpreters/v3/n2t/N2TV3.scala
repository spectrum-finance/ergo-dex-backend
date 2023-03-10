package org.ergoplatform.dex.executor.amm.interpreters.v3.n2t

import cats.effect.concurrent.Ref
import cats.{Functor, Monad}
import org.ergoplatform.dex.configs.MonetaryConfig
import org.ergoplatform.dex.domain.{DexOperatorOutput, NetworkContext}
import org.ergoplatform.dex.domain.amm.CFMMOrder._
import org.ergoplatform.dex.domain.amm.CFMMPool
import org.ergoplatform.dex.executor.amm.config.ExchangeConfig
import org.ergoplatform.dex.executor.amm.domain.errors.{EmptyOutputForDexTokenFee, ExecutionFailed}
import org.ergoplatform.dex.executor.amm.interpreters.v3.InterpreterV3
import org.ergoplatform.dex.executor.amm.interpreters.{CFMMInterpreter, CFMMInterpreterHelpers}
import org.ergoplatform.dex.executor.amm.services.DexOutputResolver
import org.ergoplatform.dex.protocol.amm.AMMContracts
import org.ergoplatform.dex.protocol.amm.AMMType.{N2T_CFMM, T2T_CFMM}
import org.ergoplatform.ergo.domain.Output
import org.ergoplatform.ergo.state.{Predicted, Traced}
import org.ergoplatform.{ErgoAddressEncoder, ErgoLikeTransaction}
import tofu.logging.Logs
import tofu.syntax.monadic._
import tofu.syntax.raise._
import cats.syntax.option._
import org.ergoplatform.dex.executor.amm.interpreters.v3.InterpreterV3.InterpreterV3Tracing

object N2TV3 {

  def make[I[_]: Functor, F[_]: Monad: ExecutionFailed.Raise](
    exchange: ExchangeConfig,
    monetary: MonetaryConfig,
    ref: Ref[F, NetworkContext]
  )(implicit
    contracts: AMMContracts[N2T_CFMM],
    encoder: ErgoAddressEncoder,
    resolver: DexOutputResolver[F],
    logs: Logs[I, F]
  ): I[InterpreterV3[N2T_CFMM, F]] =
    logs.forService[CFMMInterpreter[N2T_CFMM, F]].map { implicit l =>
      val helpers  = new CFMMInterpreterHelpers(exchange, monetary)
      val depositI = new N2TDepositTokenFeeInterpreter[F](exchange, monetary, ref, helpers)
      val redeemI  = new N2TRedeemTokenFeeInterpreter[F](exchange, monetary, ref, helpers)
      val swapI    = new N2TSwapTokenFeeInterpreter[F](exchange, monetary, ref, helpers)

      new InterpreterV3Tracing[N2T_CFMM, F] attach new InterpreterV3[N2T_CFMM, F] {
        def deposit(
          deposit: DepositTokenFee,
          pool: CFMMPool
        ): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]], Traced[Predicted[DexOperatorOutput]])] =
          resolver.getLatest
            .flatMap(_.orRaise[F](EmptyOutputForDexTokenFee(pool.poolId, deposit.box.boxId)))
            .flatMap(depositI.deposit(deposit, pool, _))

        def redeem(
          redeem: RedeemTokenFee,
          pool: CFMMPool
        ): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]], Traced[Predicted[DexOperatorOutput]])] =
          resolver.getLatest
            .flatMap(_.orRaise[F](EmptyOutputForDexTokenFee(pool.poolId, redeem.box.boxId)))
            .flatMap(redeemI.redeem(redeem, pool, _))

        def swap(
          swap: SwapTokenFee,
          pool: CFMMPool
        ): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]], Traced[Predicted[DexOperatorOutput]])] =
          resolver.getLatest
            .flatMap(_.orRaise[F](EmptyOutputForDexTokenFee(pool.poolId, swap.box.boxId)))
            .flatMap(swapI.swap(swap, pool, _))
      }
    }

}
