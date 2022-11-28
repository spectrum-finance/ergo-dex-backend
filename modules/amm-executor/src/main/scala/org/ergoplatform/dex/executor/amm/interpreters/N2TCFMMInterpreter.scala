package org.ergoplatform.dex.executor.amm.interpreters

import cats.syntax.either._
import cats.{Functor, Monad}
import org.ergoplatform._
import org.ergoplatform.dex.configs.MonetaryConfig
import org.ergoplatform.dex.domain.amm.CFMMOrder.{AnyRedeem, DepositErgFee, RedeemErgFee, SwapMultiAddress, SwapP2Pk}
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.domain.{BoxInfo, NetworkContext}
import org.ergoplatform.dex.executor.amm.config.ExchangeConfig
import org.ergoplatform.dex.executor.amm.domain.errors.{ExecutionFailed, IncorrectMultiAddressTree}
import org.ergoplatform.dex.executor.amm.interpreters.CFMMInterpreter.CFMMInterpreterTracing
import org.ergoplatform.dex.protocol.ErgoTreeSerializer
import org.ergoplatform.dex.protocol.amm.AMMType.N2T_CFMM
import org.ergoplatform.dex.protocol.amm.{AMMContracts, InterpreterVersion}
import org.ergoplatform.ergo.BoxId
import org.ergoplatform.ergo.services.explorer.ErgoExplorer
import org.ergoplatform.ergo.state.{Predicted, Traced}
import org.ergoplatform.ergo.syntax._
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
) extends CFMMInterpreter[N2T_CFMM, InterpreterVersion.V1, F] {

  val helpers = new CFMMInterpreterHelpers(exchange, execution)

  import helpers._

  def deposit(
    in: CFMMOrder.AnyDeposit,
    pool: CFMMPool
  ): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])] =
    in match {
      case deposit: DepositErgFee =>
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
        val minerFee    = execution.minerFee min deposit.maxMinerFee
        val minerFeeBox = new ErgoBoxCandidate(minerFee, minerFeeProp, ctx.currentHeight)
        val dexFee      = deposit.params.dexFee - minerFee
        val dexFeeBox   = new ErgoBoxCandidate(dexFee, dexFeeProp, ctx.currentHeight)

        val returnBox = new ErgoBoxCandidate(
          value          = depositBox.value - inX.value - minerFeeBox.value - dexFeeBox.value + changeX,
          ergoTree       = deposit.params.redeemer.toErgoTree,
          creationHeight = ctx.currentHeight,
          additionalTokens =
            if (changeY > 0) mkTokens(rewardLP.id -> rewardLP.value, inY.id -> changeY)
            else mkTokens(rewardLP.id             -> rewardLP.value)
        )
        val inputs      = Vector(poolIn, depositIn)
        val outs        = Vector(poolBox1, returnBox, dexFeeBox, minerFeeBox)
        val tx          = ErgoLikeTransaction(inputs, outs)
        val nextPoolBox = poolBox1.toBox(tx.id, 0)
        val boxInfo     = BoxInfo(BoxId.fromErgo(nextPoolBox.id), nextPoolBox.value)
        val nextPool    = pool.deposit(inX, inY, boxInfo)
        (tx, nextPool).pure
    }

  def redeem(
    in: AnyRedeem,
    pool: CFMMPool
  ): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])] = in match {
    case redeem: RedeemErgFee =>
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
      val minerFee    = execution.minerFee min redeem.maxMinerFee
      val minerFeeBox = new ErgoBoxCandidate(minerFee, minerFeeProp, ctx.currentHeight)
      val dexFee      = redeem.params.dexFee - minerFee
      val dexFeeBox   = new ErgoBoxCandidate(dexFee, dexFeeProp, ctx.currentHeight)
      val returnBox = new ErgoBoxCandidate(
        value            = redeemBox.value + shareX.value - minerFeeBox.value - dexFeeBox.value,
        ergoTree         = redeem.params.redeemer.toErgoTree,
        creationHeight   = ctx.currentHeight,
        additionalTokens = mkTokens(shareY.id -> shareY.value)
      )
      val inputs      = Vector(poolIn, redeemIn)
      val outs        = Vector(poolBox1, returnBox, dexFeeBox, minerFeeBox)
      val tx          = ErgoLikeTransaction(inputs, outs)
      val nextPoolBox = poolBox1.toBox(tx.id, 0)
      val boxInfo     = BoxInfo(BoxId.fromErgo(nextPoolBox.id), nextPoolBox.value)
      val nextPool    = pool.redeem(inLP, boxInfo)
      (tx, nextPool).pure
  }

  def swap(swap: CFMMOrder.AnySwap, pool: CFMMPool): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])] =
    swapParamsErgFee(swap, pool).toRaise.flatMap { case (input, output, dexFee) =>
      (swap match {
        case s: SwapP2Pk =>
          (s.maxMinerFee, s.params.baseAmount, s.params.redeemer.toErgoTree, s.params.minQuoteAmount).pure[F]
        case s: SwapMultiAddress =>
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
        val minerFee       = execution.minerFee min maxMinerFee
        val minerFeeBox    = new ErgoBoxCandidate(minerFee, minerFeeProp, ctx.currentHeight)
        val dexFeeBox      = if (dexFee > 0) Some(new ErgoBoxCandidate(dexFee, dexFeeProp, ctx.currentHeight)) else None
        val dexFeeBoxValue = dexFeeBox.map(_.value).getOrElse(0L)
        val rewardBox =
          if (inputSwap.isNative)
            new ErgoBoxCandidate(
              value            = swapBox.value - input.value - minerFeeBox.value - dexFeeBoxValue,
              ergoTree         = redeemer,
              creationHeight   = ctx.currentHeight,
              additionalTokens = mkTokens(minOutput.id -> output.value)
            )
          else
            new ErgoBoxCandidate(
              value          = swapBox.value + output.value - minerFeeBox.value - dexFee,
              ergoTree       = redeemer,
              creationHeight = ctx.currentHeight
            )
        val inputs      = Vector(poolIn, swapIn)
        val outs        = Vector(poolBox1, rewardBox) ++ dexFeeBox ++ Vector(minerFeeBox)
        val tx          = ErgoLikeTransaction(inputs, outs)
        val nextPoolBox = poolBox1.toBox(tx.id, 0)
        val boxInfo     = BoxInfo(BoxId.fromErgo(nextPoolBox.id), nextPoolBox.value)
        val nextPool    = pool.swap(input, boxInfo)
        tx -> nextPool
      }
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
    network: ErgoExplorer[F],
    contracts: AMMContracts[N2T_CFMM],
    encoder: ErgoAddressEncoder,
    logs: Logs[I, F]
  ): I[CFMMInterpreter[N2T_CFMM, InterpreterVersion.V1, F]] =
    logs.forService[CFMMInterpreter[N2T_CFMM, InterpreterVersion.V1, F]].map { implicit l =>
      (
        for {
          exchange   <- ExchangeConfig.access
          execution  <- MonetaryConfig.access
          networkCtx <- NetworkContext.make
        } yield new CFMMInterpreterTracing[N2T_CFMM, InterpreterVersion.V1, F] attach new N2TCFMMInterpreter(
          exchange,
          execution,
          networkCtx
        )
      ).embed
    }
}
