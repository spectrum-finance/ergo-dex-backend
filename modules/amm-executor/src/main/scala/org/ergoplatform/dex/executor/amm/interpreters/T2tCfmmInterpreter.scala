package org.ergoplatform.dex.executor.amm.interpreters

import cats.Monad
import org.ergoplatform.ErgoBox.{NonMandatoryRegisterId, R4}
import org.ergoplatform._
import org.ergoplatform.dex.TokenId
import org.ergoplatform.dex.domain.NetworkContext
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.domain.syntax.ergo._
import org.ergoplatform.dex.executor.amm.config.ExchangeConfig
import org.ergoplatform.dex.executor.amm.domain.errors.{ExecutionFailed, NoSuchPool, TooMuchSlippage}
import org.ergoplatform.dex.executor.amm.repositories.CfmmPools
import org.ergoplatform.dex.protocol.amm.AmmContractType.T2tCfmm
import org.ergoplatform.dex.protocol.amm.AmmContracts
import org.ergoplatform.network.ErgoNetwork
import sigmastate.Values.LongConstant
import sigmastate.eval._
import sigmastate.interpreter.ProverResult
import tofu.syntax.context._
import tofu.syntax.embed._
import tofu.syntax.monadic._
import tofu.syntax.raise._

final class T2tCfmmInterpreter[F[_]: Monad: ExecutionFailed.Raise](
  conf: ExchangeConfig,
  ctx: NetworkContext
)(implicit
  pools: CfmmPools[F],
  contracts: AmmContracts[T2tCfmm],
  encoder: ErgoAddressEncoder
) extends CfmmInterpreter[T2tCfmm, F] {

  def deposit(deposit: Deposit): F[ErgoLikeTransaction] =
    getPool(deposit.params.poolId) >>= { pool =>
      val poolBox0   = pool.box
      val depositBox = deposit.box
      val redeemIn   = new Input(depositBox.boxId.toErgo, ProverResult.empty)
      val poolIn     = new Input(poolBox0.boxId.toErgo, ProverResult.empty)
      val rewardLP   = pool.rewardLP(deposit.params.inX, deposit.params.inY)
      val poolBox1 = new ErgoBoxCandidate(
        value          = poolBox0.value,
        ergoTree       = contracts.pool,
        creationHeight = ctx.currentHeight,
        additionalTokens = mkPoolTokens(
          pool,
          amountLP = pool.lp.value - rewardLP.value,
          amountX  = pool.x.value + deposit.params.inX.value,
          amountY  = pool.y.value + deposit.params.inY.value
        ),
        additionalRegisters = mkPoolRegs(pool)
      )
      val minerFeeBox = new ErgoBoxCandidate(conf.minerFee, minerFeeProp, ctx.currentHeight)
      val dexFee      = deposit.params.dexFee - conf.minerFee
      val dexFeeBox   = new ErgoBoxCandidate(dexFee, dexFeeProp, ctx.currentHeight)
      val returnBox = new ErgoBoxCandidate(
        value            = depositBox.value - minerFeeBox.value - dexFeeBox.value,
        ergoTree         = deposit.params.p2pk.toErgoTree,
        creationHeight   = ctx.currentHeight,
        additionalTokens = mkTokens(rewardLP.id -> rewardLP.value)
      )
      val inputs = Vector(poolIn, redeemIn)
      val outs   = Vector(poolBox1, returnBox, dexFeeBox, minerFeeBox)
      ErgoLikeTransaction(inputs, outs).pure
    }

  def redeem(redeem: Redeem): F[ErgoLikeTransaction] =
    getPool(redeem.params.poolId) >>= { pool =>
      val poolBox0         = pool.box
      val redeemBox        = redeem.box
      val redeemIn         = new Input(redeemBox.boxId.toErgo, ProverResult.empty)
      val poolIn           = new Input(poolBox0.boxId.toErgo, ProverResult.empty)
      val (shareX, shareY) = pool.shares(redeem.params.lp)
      val poolBox1 = new ErgoBoxCandidate(
        value          = poolBox0.value,
        ergoTree       = contracts.pool,
        creationHeight = ctx.currentHeight,
        additionalTokens = mkPoolTokens(
          pool,
          amountLP = pool.lp.value + redeem.params.lp.value,
          amountX  = pool.x.value - shareX.value,
          amountY  = pool.y.value - shareY.value
        ),
        additionalRegisters = mkPoolRegs(pool)
      )
      val minerFeeBox = new ErgoBoxCandidate(conf.minerFee, minerFeeProp, ctx.currentHeight)
      val dexFee      = redeem.params.dexFee - conf.minerFee
      val dexFeeBox   = new ErgoBoxCandidate(dexFee, dexFeeProp, ctx.currentHeight)
      val returnBox = new ErgoBoxCandidate(
        value          = redeemBox.value - minerFeeBox.value - dexFeeBox.value,
        ergoTree       = redeem.params.p2pk.toErgoTree,
        creationHeight = ctx.currentHeight,
        additionalTokens = mkTokens(
          shareX.id -> shareX.value,
          shareY.id -> shareY.value
        )
      )
      val inputs = Vector(poolIn, redeemIn)
      val outs   = Vector(poolBox1, returnBox, dexFeeBox, minerFeeBox)
      ErgoLikeTransaction(inputs, outs).pure
    }

  def swap(swap: Swap): F[ErgoLikeTransaction] =
    getPool(swap.params.poolId) >>= { pool =>
      val outputAmount = pool.outputAmount(swap.params.input)
      if (outputAmount >= swap.params.minOutput) {
        val poolBox0 = pool.box
        val swapBox  = swap.box
        val swapIn   = new Input(swapBox.boxId.toErgo, ProverResult.empty)
        val poolIn   = new Input(poolBox0.boxId.toErgo, ProverResult.empty)
        val output   = pool.outputAmount(swap.params.input)
        val (deltaX, deltaY) =
          if (swap.params.input.id == pool.x.id) swap.params.input.value -> -output.value
          else -output.value                                             -> swap.params.input.value
        val poolBox1 = new ErgoBoxCandidate(
          value          = poolBox0.value,
          ergoTree       = contracts.pool,
          creationHeight = ctx.currentHeight,
          additionalTokens = mkPoolTokens(
            pool,
            amountLP = pool.lp.value,
            amountX  = pool.x.value + deltaX,
            amountY  = pool.y.value + deltaY
          ),
          additionalRegisters = mkPoolRegs(pool)
        )
        val minerFeeBox = new ErgoBoxCandidate(conf.minerFee, minerFeeProp, ctx.currentHeight)
        val dexFee      = output.value * swap.params.dexFeePerToken - conf.minerFee
        val dexFeeBox   = new ErgoBoxCandidate(dexFee, dexFeeProp, ctx.currentHeight)
        val rewardBox = new ErgoBoxCandidate(
          value            = swapBox.value - minerFeeBox.value - dexFeeBox.value,
          ergoTree         = swap.params.p2pk.toErgoTree,
          creationHeight   = ctx.currentHeight,
          additionalTokens = mkTokens(swap.params.minOutput.id -> swap.params.minOutput.value)
        )
        val inputs = Vector(poolIn, swapIn)
        val outs   = Vector(poolBox1, rewardBox, dexFeeBox, minerFeeBox)
        ErgoLikeTransaction(inputs, outs).pure
      } else TooMuchSlippage(swap.params.poolId, swap.params.minOutput, outputAmount).raise
    }

  private val minerFeeProp = Pay2SAddress(ErgoScriptPredef.feeProposition()).script
  private val dexFeeProp   = conf.rewardAddress.toErgoTree

  private def mkPoolTokens(pool: CfmmPool, amountLP: Long, amountX: Long, amountY: Long) =
    mkTokens(
      pool.poolId.value -> 1L,
      pool.lp.id        -> amountLP,
      pool.x.id         -> amountX,
      pool.y.id         -> amountY
    )

  private def mkTokens(tokens: (TokenId, Long)*) =
    Colls.fromItems(tokens.map { case (k, v) => k.toErgo -> v }: _*)

  private def mkPoolRegs(pool: CfmmPool) =
    scala.Predef.Map(
      (R4: NonMandatoryRegisterId) -> LongConstant(pool.poolFee)
    )

  private def getPool: PoolId => F[CfmmPool] =
    poolId => pools.get(poolId) >>= (_.orRaise(NoSuchPool(poolId)))
}

object T2tCfmmInterpreter {

  def make[F[_]: Monad: ExecutionFailed.Raise: ExchangeConfig.Has](implicit
    network: ErgoNetwork[F],
    pools: CfmmPools[F],
    contracts: AmmContracts[T2tCfmm],
    encoder: ErgoAddressEncoder
  ): CfmmInterpreter[T2tCfmm, F] =
    (
      for {
        conf       <- context
        networkCtx <- NetworkContext.make
      } yield new T2tCfmmInterpreter(conf, networkCtx): CfmmInterpreter[T2tCfmm, F]
    ).embed
}
