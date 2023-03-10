package org.ergoplatform.dex.executor.amm.interpreters.v3.t2t

import cats.Monad
import cats.effect.concurrent.Ref
import cats.syntax.either._
import org.ergoplatform._
import org.ergoplatform.dex.configs.MonetaryConfig
import org.ergoplatform.dex.domain.amm.CFMMOrder._
import org.ergoplatform.dex.domain.amm.CFMMPool
import org.ergoplatform.dex.domain.{BoxInfo, DexOperatorOutput, NetworkContext}
import org.ergoplatform.dex.executor.amm.config.ExchangeConfig
import org.ergoplatform.dex.executor.amm.domain.errors.{ExecutionFailed, IncorrectMultiAddressTree}
import org.ergoplatform.dex.executor.amm.interpreters.CFMMInterpreterHelpers
import org.ergoplatform.dex.protocol.ErgoTreeSerializer
import org.ergoplatform.dex.protocol.amm.AMMContracts
import org.ergoplatform.dex.protocol.amm.AMMType.T2T_CFMM
import org.ergoplatform.ergo.BoxId
import org.ergoplatform.ergo.domain.Output
import org.ergoplatform.ergo.state.{Predicted, Traced}
import org.ergoplatform.ergo.syntax._
import org.ergoplatform.wallet.interpreter.ErgoUnsafeProver
import sigmastate.interpreter.ProverResult
import tofu.syntax.monadic._
import tofu.syntax.raise._
import cats.syntax.semigroup._

class T2TDepositTokenFeeInterpreter[F[_]: Monad: ExecutionFailed.Raise](
  exchange: ExchangeConfig,
  monetary: MonetaryConfig,
  ref: Ref[F, NetworkContext],
  helpers: CFMMInterpreterHelpers
)(implicit contracts: AMMContracts[T2T_CFMM], e: ErgoAddressEncoder) {
  import helpers._

  def deposit(
    deposit: DepositTokenFee,
    pool: CFMMPool,
    dexFeeOutput: Output
  ): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]], Traced[Predicted[DexOperatorOutput]])] =
    ref.get.flatMap { ctx =>
      Either
        .catchNonFatal(ErgoTreeSerializer.default.deserialize(deposit.params.redeemer))
        .leftMap(s => IncorrectMultiAddressTree(pool.poolId, deposit.box.boxId, deposit.params.redeemer, s.getMessage))
        .toRaise
        .map { redeemer =>
          val poolBox0   = pool.box
          val depositBox = deposit.box

          val depositIn = new Input(depositBox.boxId.toErgo, ProverResult.empty)
          val poolIn    = new Input(poolBox0.boxId.toErgo, ProverResult.empty)
          val dexFeeIn  = new Input(dexFeeOutput.boxId.toErgo, ProverResult.empty)

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

          val feeChange =
            if (inX.id == helpers.exchange.spectrumToken) changeX
            else if (inY.id == helpers.exchange.spectrumToken) changeY
            else 0

          val dexFee = deposit.params.dexFee - feeChange
          val dexFeeTokensReturn: Seq[(ergo.TokenId, Long)] =
            (Map(exchange.spectrumToken -> dexFee) |+| dexFeeOutput.assets
              .map(asset => asset.tokenId -> asset.amount)
              .toMap).toSeq
          val dexFeeBox = new ErgoBoxCandidate(
            dexFeeOutput.value - minerFeeBox.value,
            P2PKAddress(sk.publicImage).script,
            ctx.currentHeight,
            additionalTokens = mkTokens(dexFeeTokensReturn: _*)
          )

          val returnBox = new ErgoBoxCandidate(
            value          = depositBox.value,
            ergoTree       = redeemer,
            creationHeight = ctx.currentHeight,
            additionalTokens =
              if (changeX > 0)
                mkTokens(rewardLP.id -> rewardLP.value, inX.id -> changeX)
              else if (changeY > 0)
                mkTokens(rewardLP.id -> rewardLP.value, inY.id -> changeY)
              else
                mkTokens(rewardLP.id -> rewardLP.value)
          )

          val inputs             = Vector(poolIn, depositIn, dexFeeIn)
          val outs               = Vector(poolBox1, returnBox, dexFeeBox, minerFeeBox)
          val tx                 = ErgoUnsafeProver.prove(UnsignedErgoLikeTransaction(inputs, outs), sk)
          val nextPoolBox        = poolBox1.toBox(tx.id, 0)
          val boxInfo            = BoxInfo(BoxId.fromErgo(nextPoolBox.id), nextPoolBox.value)
          val nextPool           = pool.deposit(inX, inY, boxInfo)
          val predictedDexOutput = Output.predicted(Output.fromErgoBox(tx.outputs(2)), dexFeeOutput.boxId)
          (tx, nextPool, predictedDexOutput)
        }
    }
}
