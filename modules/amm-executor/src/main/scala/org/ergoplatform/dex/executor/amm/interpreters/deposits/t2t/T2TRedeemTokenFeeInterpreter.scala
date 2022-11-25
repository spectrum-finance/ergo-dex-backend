package org.ergoplatform.dex.executor.amm.interpreters.deposits.t2t

import cats.Functor
import cats.effect.concurrent.Ref
import org.ergoplatform._
import org.ergoplatform.dex.configs.MonetaryConfig
import org.ergoplatform.dex.domain.amm.CFMMOrder._
import org.ergoplatform.dex.domain.amm.CFMMPool
import org.ergoplatform.dex.domain.{BoxInfo, NetworkContext}
import org.ergoplatform.dex.executor.amm.config.ExchangeConfig
import org.ergoplatform.dex.executor.amm.interpreters.CFMMInterpreterHelpers
import org.ergoplatform.dex.protocol.amm.AMMContracts
import org.ergoplatform.dex.protocol.amm.AMMType.N2T_CFMM
import org.ergoplatform.ergo.BoxId
import org.ergoplatform.ergo.domain.Output
import org.ergoplatform.ergo.state.{Predicted, Traced}
import org.ergoplatform.ergo.syntax._
import org.ergoplatform.wallet.interpreter.ErgoUnsafeProver
import sigmastate.interpreter.ProverResult
import tofu.syntax.monadic._

final class T2TRedeemTokenFeeInterpreter[F[_]: Functor](
  exchange: ExchangeConfig,
  monetary: MonetaryConfig,
  ref: Ref[F, NetworkContext],
  helpers: CFMMInterpreterHelpers
)(implicit contracts: AMMContracts[N2T_CFMM]) {

  import helpers._

  def redeem(
    redeem: RedeemErgFee,
    pool: CFMMPool,
    dexFeeOutput: Output
  ): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]])] = ref.get.map { ctx =>
    val poolBox0         = pool.box
    val redeemBox        = redeem.box
    val redeemIn         = new Input(redeemBox.boxId.toErgo, ProverResult.empty)
    val poolIn           = new Input(poolBox0.boxId.toErgo, ProverResult.empty)
    val dexFeeIn         = new Input(dexFeeOutput.boxId.toErgo, ProverResult.empty)
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
    val dexFee      = redeem.params.dexFee
    val dexFeeBox = new ErgoBoxCandidate(
      dexFeeOutput.value,
      dexFeeProp,
      ctx.currentHeight,
      additionalTokens = mkTokens(exchange.spectrumToken -> dexFee)
    )
    val returnBox = new ErgoBoxCandidate(
      value          = redeemBox.value - minerFeeBox.value,
      ergoTree       = redeem.params.redeemer.toErgoTree,
      creationHeight = ctx.currentHeight,
      additionalTokens = mkTokens(
        shareX.id -> shareX.value,
        shareY.id -> shareY.value
      )
    )

    val dexInput =
      Vector(
        ErgoUnsafeProver
          .prove(UnsignedErgoLikeTransaction(IndexedSeq(dexFeeIn), IndexedSeq.empty), exchange.sk)
          .inputs
          .headOption
      ).flatten

    val inputs      = Vector(poolIn, redeemIn) ++ dexInput
    val outs        = Vector(poolBox1, returnBox, dexFeeBox, minerFeeBox)
    val tx          = ErgoLikeTransaction(inputs, outs)
    val nextPoolBox = poolBox1.toBox(tx.id, 0)
    val boxInfo     = BoxInfo(BoxId.fromErgo(nextPoolBox.id), nextPoolBox.value)
    val nextPool    = pool.redeem(inLP, boxInfo)
    (tx, nextPool)
  }

}
