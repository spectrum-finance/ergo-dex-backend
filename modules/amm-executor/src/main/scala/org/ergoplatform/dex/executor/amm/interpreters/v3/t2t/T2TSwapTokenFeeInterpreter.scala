package org.ergoplatform.dex.executor.amm.interpreters.v3.t2t

import cats.{Functor, Monad}
import cats.effect.concurrent.Ref
import cats.syntax.either._
import org.bouncycastle.util.BigIntegers
import org.ergoplatform.dex.configs.MonetaryConfig
import org.ergoplatform.dex.domain.amm.CFMMOrder._
import org.ergoplatform.dex.domain.amm.CFMMPool
import org.ergoplatform.dex.domain.{BoxInfo, NetworkContext}
import org.ergoplatform.dex.executor.amm.config.ExchangeConfig
import org.ergoplatform.dex.executor.amm.domain.errors.{ExecutionFailed, IncorrectMultiAddressTree}
import org.ergoplatform.dex.executor.amm.interpreters.CFMMInterpreterHelpers
import org.ergoplatform.dex.protocol.ErgoTreeSerializer
import org.ergoplatform.dex.protocol.amm.AMMContracts
import org.ergoplatform.dex.protocol.amm.AMMType.N2T_CFMM
import org.ergoplatform.ergo.BoxId
import org.ergoplatform.ergo.domain.Output
import org.ergoplatform.ergo.state.{Predicted, Traced}
import org.ergoplatform.ergo.syntax._
import org.ergoplatform.wallet.interpreter.ErgoUnsafeProver
import org.ergoplatform.{ErgoBoxCandidate, ErgoLikeTransaction, Input, UnsignedErgoLikeTransaction}
import scorex.util.encode.Base16
import sigmastate.basics.DLogProtocol.DLogProverInput
import sigmastate.interpreter.ProverResult
import tofu.syntax.monadic._
import tofu.syntax.raise._

class T2TSwapTokenFeeInterpreter[F[_]: Monad: ExecutionFailed.Raise](
  exchange: ExchangeConfig,
  monetary: MonetaryConfig,
  ref: Ref[F, NetworkContext],
  helpers: CFMMInterpreterHelpers
)(implicit contracts: AMMContracts[N2T_CFMM]) {
  val sk: DLogProverInput = DLogProverInput(BigIntegers.fromUnsignedByteArray(Base16.decode(exchange.skHex).get))

  import helpers._

  def swap(
    swap: SwapTokenFee,
    pool: CFMMPool,
    dexFeeOutput: Output
  ): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])] = ref.get.flatMap { ctx =>
    swapParamsTokenFee(swap, pool).toRaise.flatMap { case (baseAmount, quoteAmount, dexFee) =>
      Either
        .catchNonFatal(ErgoTreeSerializer.default.deserialize(swap.params.redeemer))
        .leftMap(s => IncorrectMultiAddressTree(pool.poolId, swap.box.boxId, swap.params.redeemer, s.getMessage))
        .toRaise
        .map { redeemer =>
          val poolBox0 = pool.box
          val swapBox  = swap.box
          val swapIn   = new Input(swapBox.boxId.toErgo, ProverResult.empty)
          val poolIn   = new Input(poolBox0.boxId.toErgo, ProverResult.empty)
          val dexFeeIn = new Input(dexFeeOutput.boxId.toErgo, ProverResult.empty)
          val (deltaX, deltaY) =
            if (baseAmount.id == pool.x.id) baseAmount.value -> -quoteAmount.value
            else -quoteAmount.value                          -> baseAmount.value
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
          val minerFee    = monetary.minerFee min swap.maxMinerFee
          val minerFeeBox = new ErgoBoxCandidate(minerFee, minerFeeProp, ctx.currentHeight)

          val dexFeeBox = new ErgoBoxCandidate(
            dexFeeOutput.value,
            dexFeeProp,
            ctx.currentHeight,
            additionalTokens = mkTokens(exchange.spectrumToken -> dexFee)
          )

          val rewardBox = new ErgoBoxCandidate(
            value          = swapBox.value - minerFeeBox.value,
            ergoTree       = redeemer,
            creationHeight = ctx.currentHeight,
            additionalTokens =
              if (quoteAmount.id == exchange.spectrumToken)
                mkTokens(
                  swap.params.minQuoteAmount.id -> (quoteAmount.value + swap.reservedExFee - dexFee)
                )
              else
                mkTokens(
                  swap.params.minQuoteAmount.id -> quoteAmount.value,
                  exchange.spectrumToken        -> (swap.reservedExFee - dexFee)
                )
          )

          val dexInput =
            Vector(
              ErgoUnsafeProver
                .prove(UnsignedErgoLikeTransaction(IndexedSeq(dexFeeIn), IndexedSeq.empty), sk)
                .inputs
                .headOption
            ).flatten

          val inputs      = Vector(poolIn, swapIn) ++ dexInput
          val outs        = Vector(poolBox1, rewardBox, dexFeeBox, minerFeeBox)
          val tx          = ErgoLikeTransaction(inputs, outs)
          val nextPoolBox = poolBox1.toBox(tx.id, 0)
          val boxInfo     = BoxInfo(BoxId.fromErgo(nextPoolBox.id), nextPoolBox.value)
          val nextPool    = pool.swap(baseAmount, boxInfo)
          tx -> nextPool
        }
    }
  }

}
