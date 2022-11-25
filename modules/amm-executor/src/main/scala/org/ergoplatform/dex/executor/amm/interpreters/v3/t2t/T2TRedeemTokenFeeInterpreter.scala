package org.ergoplatform.dex.executor.amm.interpreters.v3.t2t

import cats.Monad
import cats.effect.concurrent.Ref
import cats.syntax.either._
import org.ergoplatform._
import org.ergoplatform.dex.configs.MonetaryConfig
import org.ergoplatform.dex.domain.amm.CFMMOrder._
import org.ergoplatform.dex.domain.amm.CFMMPool
import org.ergoplatform.dex.domain.{BoxInfo, NetworkContext}
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

final class T2TRedeemTokenFeeInterpreter[F[_]: Monad: ExecutionFailed.Raise](
  exchange: ExchangeConfig,
  monetary: MonetaryConfig,
  ref: Ref[F, NetworkContext],
  helpers: CFMMInterpreterHelpers
)(implicit contracts: AMMContracts[T2T_CFMM], e: ErgoAddressEncoder) {
  import helpers._

  def redeem(
    redeem: RedeemTokenFee,
    pool: CFMMPool,
    dexFeeOutput: Output
  ): F[(ErgoLikeTransaction, Traced[Predicted[CFMMPool]], Output)] = ref.get.flatMap { ctx =>
    Either
      .catchNonFatal(ErgoTreeSerializer.default.deserialize(redeem.params.redeemer))
      .leftMap(s => IncorrectMultiAddressTree(pool.poolId, redeem.box.boxId, redeem.params.redeemer, s.getMessage))
      .toRaise
      .map { redeemer =>
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
          value          = redeemBox.value,
          ergoTree       = redeemer,
          creationHeight = ctx.currentHeight,
          additionalTokens = mkTokens(
            shareX.id -> shareX.value,
            shareY.id -> shareY.value
          )
        )

        val inputs             = Vector(poolIn, redeemIn, dexFeeIn)
        val outs               = Vector(poolBox1, returnBox, dexFeeBox, minerFeeBox)
        val tx                 = ErgoUnsafeProver.prove(UnsignedErgoLikeTransaction(inputs, outs), sk)
        val nextPoolBox        = poolBox1.toBox(tx.id, 0)
        val boxInfo            = BoxInfo(BoxId.fromErgo(nextPoolBox.id), nextPoolBox.value)
        val nextPool           = pool.redeem(inLP, boxInfo)
        val predictedDexOutput = Output.fromErgoBox(tx.outputs(2))
        (tx, nextPool, predictedDexOutput)
      }
  }

}
