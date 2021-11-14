package org.ergoplatform.dex.executor.amm.interpreters

import cats.{Functor, Monad}
import org.ergoplatform._
import org.ergoplatform.dex.configs.MonetaryConfig
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.domain.amm.state.Predicted
import org.ergoplatform.dex.domain.{BoxInfo, NetworkContext}
import org.ergoplatform.dex.executor.amm.config.ExchangeConfig
import org.ergoplatform.dex.executor.amm.domain.errors.ExecutionFailed
import org.ergoplatform.dex.executor.amm.interpreters.CFMMInterpreter.CFMMInterpreterTracing
import org.ergoplatform.dex.protocol.amm.AMMContracts
import org.ergoplatform.dex.protocol.amm.AMMType.N2T_CFMM
import org.ergoplatform.ergo.syntax._
import org.ergoplatform.ergo.{BoxId, ErgoNetwork}
import sigmastate.interpreter.ProverResult
import tofu.logging.Logs
import tofu.syntax.embed._
import tofu.syntax.monadic._
import tofu.syntax.raise._

final class N2TCFMMInterpreter[F[_]: Monad: ExecutionFailed.Raise](
  exchange: ExchangeConfig,
  execution: MonetaryConfig,
  ctx: NetworkContext
)(implicit
  contracts: AMMContracts[N2T_CFMM],
  encoder: ErgoAddressEncoder
) extends CFMMInterpreter[N2T_CFMM, F] {

  val helpers = new CFMMInterpreterHelpers(exchange, execution)

  import helpers._

  def deposit(deposit: Deposit, pool: CFMMPool): F[(ErgoLikeTransaction, Predicted[CFMMPool])] = {
    val poolBox0           = pool.box
    val depositBox         = deposit.box
    val depositIn          = new Input(depositBox.boxId.toErgo, ProverResult.empty)
    val poolIn             = new Input(poolBox0.boxId.toErgo, ProverResult.empty)
    val (inX, inY)         = (deposit.params.inX, deposit.params.inY)
    val (rewardLP, change) = pool.rewardLP(inX, inY)
    val (changeX, changeY) =
      (change.filter(_.id == inX.id).map(_.value).sum, change.filter(_.id == inY.id).map(_.value).sum)
    val poolBox1 = new ErgoBoxCandidate(
      value          = poolBox0.value + inX.value - changeX,
      ergoTree       = contracts.pool,
      creationHeight = ctx.currentHeight,
      additionalTokens = mkPoolTokens(
        pool,
        amountLP = pool.lp.value - rewardLP.value,
        amountY  = pool.y.value + inY.value - changeY
      ),
      additionalRegisters = mkPoolRegs(pool)
    )
    val minerFeeBox = new ErgoBoxCandidate(execution.minerFee, minerFeeProp, ctx.currentHeight)
    val dexFee      = deposit.params.dexFee - execution.minerFee
    val dexFeeBox   = new ErgoBoxCandidate(dexFee, dexFeeProp, ctx.currentHeight)

    val returnBox = new ErgoBoxCandidate(
      value          = depositBox.value - inX.value - minerFeeBox.value - dexFeeBox.value + changeX,
      ergoTree       = deposit.params.p2pk.toErgoTree,
      creationHeight = ctx.currentHeight,
      additionalTokens =
        if (changeY > 0) mkTokens(rewardLP.id -> rewardLP.value, inY.id -> changeY)
        else mkTokens(rewardLP.id             -> rewardLP.value)
    )
    val inputs      = Vector(poolIn, depositIn)
    val outs        = Vector(poolBox1, returnBox, dexFeeBox, minerFeeBox)
    val tx          = ErgoLikeTransaction(inputs, outs)
    val nextPoolBox = poolBox1.toBox(tx.id, 0)
    val boxInfo     = BoxInfo(BoxId.fromErgo(nextPoolBox.id), nextPoolBox.value, poolBox0.lastConfirmedBoxGix)
    val nextPool    = pool.deposit(inX, inY, boxInfo)
    (tx, nextPool).pure
  }

  def redeem(redeem: Redeem, pool: CFMMPool): F[(ErgoLikeTransaction, Predicted[CFMMPool])] = {
    val poolBox0         = pool.box
    val redeemBox        = redeem.box
    val redeemIn         = new Input(redeemBox.boxId.toErgo, ProverResult.empty)
    val poolIn           = new Input(poolBox0.boxId.toErgo, ProverResult.empty)
    val inLP             = redeem.params.lp
    val (shareX, shareY) = pool.shares(inLP)
    val poolBox1 = new ErgoBoxCandidate(
      value          = poolBox0.value - shareX.value,
      ergoTree       = contracts.pool,
      creationHeight = ctx.currentHeight,
      additionalTokens = mkPoolTokens(
        pool,
        amountLP = pool.lp.value + inLP.value,
        amountY  = pool.y.value - shareY.value
      ),
      additionalRegisters = mkPoolRegs(pool)
    )
    val minerFeeBox = new ErgoBoxCandidate(execution.minerFee, minerFeeProp, ctx.currentHeight)
    val dexFee      = redeem.params.dexFee - execution.minerFee
    val dexFeeBox   = new ErgoBoxCandidate(dexFee, dexFeeProp, ctx.currentHeight)
    val returnBox = new ErgoBoxCandidate(
      value            = redeemBox.value + shareX.value - minerFeeBox.value - dexFeeBox.value,
      ergoTree         = redeem.params.p2pk.toErgoTree,
      creationHeight   = ctx.currentHeight,
      additionalTokens = mkTokens(shareY.id -> shareY.value)
    )
    val inputs      = Vector(poolIn, redeemIn)
    val outs        = Vector(poolBox1, returnBox, dexFeeBox, minerFeeBox)
    val tx          = ErgoLikeTransaction(inputs, outs)
    val nextPoolBox = poolBox1.toBox(tx.id, 0)
    val boxInfo     = BoxInfo(BoxId.fromErgo(nextPoolBox.id), nextPoolBox.value, poolBox0.lastConfirmedBoxGix)
    val nextPool    = pool.redeem(inLP, boxInfo)
    (tx, nextPool).pure
  }

  def swap(swap: Swap, pool: CFMMPool): F[(ErgoLikeTransaction, Predicted[CFMMPool])] =
    swapParams(swap, pool).toRaise.map { case (input, output, dexFee) =>
      val poolBox0 = pool.box
      val swapBox  = swap.box
      val swapIn   = new Input(swapBox.boxId.toErgo, ProverResult.empty)
      val poolIn   = new Input(poolBox0.boxId.toErgo, ProverResult.empty)
      val (deltaX, deltaY) =
        if (input.id == pool.x.id) input.value -> -output.value
        else -output.value                     -> input.value
      val poolBox1 = new ErgoBoxCandidate(
        value          = poolBox0.value + deltaX,
        ergoTree       = contracts.pool,
        creationHeight = ctx.currentHeight,
        additionalTokens = mkPoolTokens(
          pool,
          amountLP = pool.lp.value,
          amountY  = pool.y.value + deltaY
        ),
        additionalRegisters = mkPoolRegs(pool)
      )
      val minerFeeBox = new ErgoBoxCandidate(execution.minerFee, minerFeeProp, ctx.currentHeight)
      val dexFeeBox   = new ErgoBoxCandidate(dexFee, dexFeeProp, ctx.currentHeight)
      val rewardBox =
        if (swap.params.input.isNative)
          new ErgoBoxCandidate(
            value            = swapBox.value - input.value - minerFeeBox.value - dexFeeBox.value,
            ergoTree         = swap.params.p2pk.toErgoTree,
            creationHeight   = ctx.currentHeight,
            additionalTokens = mkTokens(swap.params.minOutput.id -> output.value)
          )
        else
          new ErgoBoxCandidate(
            value          = swapBox.value + output.value - minerFeeBox.value - dexFeeBox.value,
            ergoTree       = swap.params.p2pk.toErgoTree,
            creationHeight = ctx.currentHeight
          )
      val inputs      = Vector(poolIn, swapIn)
      val outs        = Vector(poolBox1, rewardBox, dexFeeBox, minerFeeBox)
      val tx          = ErgoLikeTransaction(inputs, outs)
      val nextPoolBox = poolBox1.toBox(tx.id, 0)
      val boxInfo     = BoxInfo(BoxId.fromErgo(nextPoolBox.id), nextPoolBox.value, poolBox0.lastConfirmedBoxGix)
      val nextPool    = pool.swap(input, boxInfo)
      tx -> nextPool
    }

  private def mkPoolTokens(pool: CFMMPool, amountLP: Long, amountY: Long) =
    mkTokens(
      pool.poolId.value -> 1L,
      pool.lp.id        -> amountLP,
      pool.y.id         -> amountY
    )
}

object N2TCFMMInterpreter {

  def make[I[_]: Functor, F[_]: Monad: ExecutionFailed.Raise: ExchangeConfig.Has: MonetaryConfig.Has](implicit
    network: ErgoNetwork[F],
    contracts: AMMContracts[N2T_CFMM],
    encoder: ErgoAddressEncoder,
    logs: Logs[I, F]
  ): I[CFMMInterpreter[N2T_CFMM, F]] =
    logs.forService[CFMMInterpreter[N2T_CFMM, F]].map { implicit l =>
      (
        for {
          exchange   <- ExchangeConfig.access
          execution  <- MonetaryConfig.access
          networkCtx <- NetworkContext.make
        } yield new CFMMInterpreterTracing[N2T_CFMM, F] attach new N2TCFMMInterpreter(exchange, execution, networkCtx)
      ).embed
    }
}
