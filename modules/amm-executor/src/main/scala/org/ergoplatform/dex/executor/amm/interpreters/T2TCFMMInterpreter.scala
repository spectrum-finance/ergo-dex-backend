package org.ergoplatform.dex.executor.amm.interpreters

import cats.{Functor, Monad}
import org.ergoplatform._
import org.ergoplatform.dex.configs.MonetaryConfig
import org.ergoplatform.dex.domain.amm.CFMMOrder.{Deposit, Redeem, Swap}
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.ergo.state.{Predicted, Traced}
import org.ergoplatform.dex.domain.{BoxInfo, NetworkContext}
import org.ergoplatform.dex.executor.amm.config.ExchangeConfig
import org.ergoplatform.dex.executor.amm.domain.errors.{ExecutionFailed, IncorrectMultiAddressTree}
import org.ergoplatform.dex.executor.amm.interpreters.CFMMInterpreter.CFMMInterpreterTracing
import org.ergoplatform.dex.protocol.ErgoTreeSerializer
import org.ergoplatform.dex.protocol.amm.AMMContracts
import org.ergoplatform.dex.protocol.amm.AMMType.T2T_CFMM
import org.ergoplatform.ergo.syntax._
import org.ergoplatform.ergo.{BoxId, PubKey, SErgoTree}
import org.ergoplatform.ergo.services.explorer.ErgoExplorer
import sigmastate.interpreter.ProverResult
import tofu.logging.Logs
import tofu.syntax.embed._
import tofu.syntax.monadic._
import tofu.syntax.raise._
import cats.syntax.either._
import org.ergoplatform.dex.domain.amm.CFMMOrderType.FeeType.ErgFee
import org.ergoplatform.dex.domain.amm.CFMMOrderType.SwapType

final class T2TCFMMInterpreter[F[_]: Monad: ExecutionFailed.Raise](
  exchange: ExchangeConfig,
  monetary: MonetaryConfig,
  ctx: NetworkContext
)(implicit
  contracts: AMMContracts[T2T_CFMM],
  encoder: ErgoAddressEncoder
) extends CFMMInterpreter[T2T_CFMM, F] {

  val helpers = new CFMMInterpreterHelpers(exchange, monetary)
  import helpers._

  def deposit(deposit: Deposit[ErgFee, PubKey], pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])] = {
    val poolBox0           = pool.box
    val depositBox         = deposit.box
    val depositIn          = new Input(depositBox.boxId.toErgo, ProverResult.empty)
    val poolIn             = new Input(poolBox0.boxId.toErgo, ProverResult.empty)
    val (inX, inY)         = (deposit.params.inX, deposit.params.inY)
    val (rewardLP, change) = pool.rewardLP(inX, inY)
    val (changeX, changeY) =
      (change.filter(_.id == inX.id).map(_.value).sum, change.filter(_.id == inY.id).map(_.value).sum)
    val poolBox1 = new ErgoBoxCandidate(
      value          = poolBox0.value,
      ergoTree       = contracts.pool,
      creationHeight = ctx.currentHeight,
      additionalTokens = mkPoolTokens(
        pool,
        amountLP = pool.lp.value - rewardLP.value,
        amountX  = pool.x.value + inX.value - changeX,
        amountY  = pool.y.value + inY.value - changeY
      ),
      additionalRegisters = mkPoolRegs(pool)
    )
    val minerFee    = monetary.minerFee min deposit.maxMinerFee
    val minerFeeBox = new ErgoBoxCandidate(minerFee, minerFeeProp, ctx.currentHeight)
    val dexFee      = deposit.params.dexFee - minerFee
    val dexFeeBox   = new ErgoBoxCandidate(dexFee, dexFeeProp, ctx.currentHeight)
    val returnBox = new ErgoBoxCandidate(
      value          = depositBox.value - minerFeeBox.value - dexFeeBox.value,
      ergoTree       = deposit.params.redeemer.toErgoTree,
      creationHeight = ctx.currentHeight,
      additionalTokens =
        if (changeX > 0) mkTokens(rewardLP.id -> rewardLP.value, inX.id -> changeX)
        else if (changeY > 0) mkTokens(rewardLP.id -> rewardLP.value, inY.id -> changeY)
        else mkTokens(rewardLP.id                  -> rewardLP.value)
    )
    val inputs      = Vector(poolIn, depositIn)
    val outs        = Vector(poolBox1, returnBox, dexFeeBox, minerFeeBox)
    val tx          = ErgoLikeTransaction(inputs, outs)
    val nextPoolBox = poolBox1.toBox(tx.id, 0)
    val boxInfo     = BoxInfo(BoxId.fromErgo(nextPoolBox.id), nextPoolBox.value)
    val nextPool    = pool.deposit(inX, inY, boxInfo)
    (tx, nextPool).pure
  }

  def redeem(redeem: Redeem[ErgFee, PubKey], pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])] = {
    val poolBox0         = pool.box
    val redeemBox        = redeem.box
    val redeemIn         = new Input(redeemBox.boxId.toErgo, ProverResult.empty)
    val poolIn           = new Input(poolBox0.boxId.toErgo, ProverResult.empty)
    val inLP             = redeem.params.lp
    val (shareX, shareY) = pool.shares(inLP)
    val poolBox1 = new ErgoBoxCandidate(
      value          = poolBox0.value,
      ergoTree       = contracts.pool,
      creationHeight = ctx.currentHeight,
      additionalTokens = mkPoolTokens(
        pool,
        amountLP = pool.lp.value + inLP.value,
        amountX  = pool.x.value - shareX.value,
        amountY  = pool.y.value - shareY.value
      ),
      additionalRegisters = mkPoolRegs(pool)
    )
    val minerFee    = monetary.minerFee min redeem.maxMinerFee
    val minerFeeBox = new ErgoBoxCandidate(minerFee, minerFeeProp, ctx.currentHeight)
    val dexFee      = redeem.params.dexFee - minerFee
    val dexFeeBox   = new ErgoBoxCandidate(dexFee, dexFeeProp, ctx.currentHeight)
    val returnBox = new ErgoBoxCandidate(
      value          = redeemBox.value - minerFeeBox.value - dexFeeBox.value,
      ergoTree       = redeem.params.redeemer.toErgoTree,
      creationHeight = ctx.currentHeight,
      additionalTokens = mkTokens(
        shareX.id -> shareX.value,
        shareY.id -> shareY.value
      )
    )
    val inputs      = Vector(poolIn, redeemIn)
    val outs        = Vector(poolBox1, returnBox, dexFeeBox, minerFeeBox)
    val tx          = ErgoLikeTransaction(inputs, outs)
    val nextPoolBox = poolBox1.toBox(tx.id, 0)
    val boxInfo     = BoxInfo(BoxId.fromErgo(nextPoolBox.id), nextPoolBox.value)
    val nextPool    = pool.redeem(inLP, boxInfo)
    (tx, nextPool).pure
  }

  def swap(swap: CFMMOrder.SwapErgAny, pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])] =
    swapParamsErgFee(swap, pool).toRaise.flatMap { case (input, output, dexFee) =>
      (swap match {
        case s: Swap[SwapType.SwapP2Pk, PubKey] =>
          (s.maxMinerFee, s.params.baseAmount, s.params.redeemer.toErgoTree, s.params.minQuoteAmount).pure[F]
        case s: Swap[SwapType.SwapMultiAddress, SErgoTree] =>
          Either
            .catchNonFatal(ErgoTreeSerializer.default.deserialize(s.params.redeemer))
            .leftMap(err => IncorrectMultiAddressTree(pool.poolId, s.box.boxId, s.params.redeemer, err.getMessage))
            .toRaise
            .map(tree => (s.maxMinerFee, s.params.baseAmount, tree, s.params.minQuoteAmount))
      }).map { case (maxMinerFee, inputSwap, redeemer, minOutput) =>
        val poolBox0 = pool.box
        val swapBox  = swap.box
        val swapIn   = new Input(swapBox.boxId.toErgo, ProverResult.empty)
        val poolIn   = new Input(poolBox0.boxId.toErgo, ProverResult.empty)
        val (deltaX, deltaY) =
          if (input.id == pool.x.id) input.value -> -output.value
          else -output.value                     -> input.value
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
        val minerFee    = monetary.minerFee min maxMinerFee
        val minerFeeBox = new ErgoBoxCandidate(minerFee, minerFeeProp, ctx.currentHeight)
        val dexFeeBox   = new ErgoBoxCandidate(dexFee, dexFeeProp, ctx.currentHeight)
        val rewardBox = new ErgoBoxCandidate(
          value            = swapBox.value - minerFeeBox.value - dexFeeBox.value,
          ergoTree         = redeemer,
          creationHeight   = ctx.currentHeight,
          additionalTokens = mkTokens(minOutput.id -> output.value)
        )
        val inputs      = Vector(poolIn, swapIn)
        val outs        = Vector(poolBox1, rewardBox, dexFeeBox, minerFeeBox)
        val tx          = ErgoLikeTransaction(inputs, outs)
        val nextPoolBox = poolBox1.toBox(tx.id, 0)
        val boxInfo     = BoxInfo(BoxId.fromErgo(nextPoolBox.id), nextPoolBox.value)
        val nextPool    = pool.swap(input, boxInfo)
        tx -> nextPool
      }
    }

  private def mkPoolTokens(pool: CFMMPool, amountLP: Long, amountX: Long, amountY: Long) =
    mkTokens(
      pool.poolId.value -> 1L,
      pool.lp.id        -> amountLP,
      pool.x.id         -> amountX,
      pool.y.id         -> amountY
    )
}

object T2TCFMMInterpreter {

  def make[I[_]: Functor, F[_]: Monad: ExecutionFailed.Raise: ExchangeConfig.Has: MonetaryConfig.Has](implicit
    network: ErgoExplorer[F],
    contracts: AMMContracts[T2T_CFMM],
    encoder: ErgoAddressEncoder,
    logs: Logs[I, F]
  ): I[CFMMInterpreter[T2T_CFMM, F]] =
    logs.forService[CFMMInterpreter[T2T_CFMM, F]].map { implicit l =>
      (
        for {
          exchange   <- ExchangeConfig.access
          execution  <- MonetaryConfig.access
          networkCtx <- NetworkContext.make
        } yield new CFMMInterpreterTracing[T2T_CFMM, F] attach new T2TCFMMInterpreter(exchange, execution, networkCtx)
      ).embed
    }
}
