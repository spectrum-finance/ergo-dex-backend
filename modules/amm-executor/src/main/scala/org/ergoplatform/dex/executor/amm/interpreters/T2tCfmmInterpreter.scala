package org.ergoplatform.dex.executor.amm.interpreters

import cats.Monad
import org.ergoplatform.ErgoBox.{NonMandatoryRegisterId, R4}
import org.ergoplatform._
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.domain.syntax.ergo._
import org.ergoplatform.dex.executor.amm.domain.NetworkContext
import org.ergoplatform.dex.executor.amm.domain.errors.{ExecutionFailed, NoSuchPool, TooMuchSlippage}
import org.ergoplatform.dex.executor.amm.repositories.T2tCfmmPools
import org.ergoplatform.dex.protocol.amm.AmmContractType.T2tCfmm
import org.ergoplatform.dex.protocol.amm.AmmContracts
import sigmastate.Values.LongConstant
import sigmastate.eval._
import sigmastate.interpreter.ProverResult
import tofu.syntax.monadic._
import tofu.syntax.raise._

final class T2tCfmmInterpreter[F[_]: Monad: ExecutionFailed.Raise](ctx: NetworkContext)(implicit
  pools: T2tCfmmPools[F],
  contracts: AmmContracts[T2tCfmm],
  encoder: ErgoAddressEncoder
) extends CfmmInterpreter[T2tCfmm, F] {

  def deposit(op: Deposit): F[ErgoLikeTransaction] = ???

  def redeem(op: Redeem): F[ErgoLikeTransaction] = ???

  def swap(swap: Swap): F[ErgoLikeTransaction] =
    getPool(swap.poolId) >>= { pool =>
      val outputAmount = pool.outputAmount(swap.input)
      if (outputAmount >= swap.minOutput) {
        val poolBox0 = pool.box
        val swapBox  = swap.box
        val swapIn   = new Input(swapBox.boxId.toErgo, ProverResult.empty)
        val poolIn   = new Input(poolBox0.boxId.toErgo, ProverResult.empty)
        val output   = pool.outputAmount(swap.input)
        val (deltaX, deltaY) =
          if (swap.input.id == pool.x.id) swap.input.amount -> -output.amount
          else -output.amount                               -> swap.input.amount
        val poolBox1 = new ErgoBoxCandidate(
          poolBox0.value,
          contracts.pool,
          ctx.currentHeight,
          additionalTokens = Colls.fromItems(
            pool.poolId.value.toErgo -> 1L,
            pool.lp.id.toErgo        -> pool.lp.amount,
            pool.x.id.toErgo         -> (pool.x.amount + deltaX),
            pool.y.id.toErgo         -> (pool.y.amount + deltaY)
          ),
          additionalRegisters = scala.Predef.Map(
            (R4: NonMandatoryRegisterId) -> LongConstant(pool.poolFee)
          )
        )
        val feeAddress  = Pay2SAddress(ErgoScriptPredef.feeProposition())
        val minerFeeBox = new ErgoBoxCandidate(swap.minerFee, feeAddress.script, ctx.currentHeight)
        val rewardBox = new ErgoBoxCandidate(
          swapBox.value - minerFeeBox.value,
          swapBox.address.toErgoTree,
          ctx.currentHeight,
          additionalTokens = Colls.fromItems(swap.minOutput.id.toErgo -> swap.minOutput.amount)
        )
        val inputs = Vector(poolIn, swapIn)
        val outs   = Vector(poolBox1, rewardBox, minerFeeBox)
        ErgoLikeTransaction(inputs, outs).pure
      } else TooMuchSlippage(swap.poolId, swap.minOutput, outputAmount).raise
    }

  private def getPool: PoolId => F[T2tCfmmPool] =
    poolId => pools.get(poolId) >>= (_.orRaise(NoSuchPool(poolId)))
}
