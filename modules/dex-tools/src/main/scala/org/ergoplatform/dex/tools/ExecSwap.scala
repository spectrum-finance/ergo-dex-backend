package org.ergoplatform.dex.tools

import cats.effect.{Clock, ExitCode, IO, IOApp}
import io.circe.syntax.EncoderOps
import org.ergoplatform.{UnsignedErgoLikeTransaction, UnsignedInput}
import org.ergoplatform.dex.configs.MonetaryConfig
import org.ergoplatform.dex.domain.amm.CFMMOrder.Swap
import org.ergoplatform.dex.domain.{AssetAmount, NetworkContext}
import org.ergoplatform.dex.domain.amm.SwapParams
import org.ergoplatform.dex.executor.amm.config.ExchangeConfig
import org.ergoplatform.dex.executor.amm.interpreters.N2TCFMMInterpreter
import org.ergoplatform.dex.tracker.parsers.amm.N2TCFMMPoolsParser
import org.ergoplatform.ergo.{Address, PubKey, TokenId}
import org.ergoplatform.wallet.interpreter.ErgoUnsafeProver
import org.ergoplatform.dex.protocol.codecs._

import java.util.concurrent.TimeUnit

object ExecSwap extends IOApp with SigmaPlatform {

  def secretHex: String = ""

  val userInId = "09accba7f3dd4806a7ba941810a85193c95ddf7550bd48945b4ec68519774aeb"
  val poolInId = "5581df1cb02f5f99aab2cad20fb15c97e081fc68fe11c5fc17cf055bad123346"

  val RecvAddr: Address = Address.fromStringUnsafe("9hqBvFiHGCimMpwG5t1KbGhHXKkx5c41RNvFtjotSZ4Lh7pXLGM")
  val SigUSD: TokenId   = TokenId.fromStringUnsafe("03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04")

  val monetaryConfig: MonetaryConfig = MonetaryConfig(1000000L, 0L, 60000L)

  def run(args: List[String]): IO[ExitCode] =
    for {
      curHeight <- IO.delay(currentHeight())
      userIn    <- IO.delay(getInput(userInId).get)
      poolIn    <- IO.delay(getInput(poolInId).get)

      ts <- Clock[IO].realTime(TimeUnit.MILLISECONDS)

      pool = N2TCFMMPoolsParser.pool(poolIn).get
      swap = SwapParams[PubKey](
               baseAmount               = AssetAmount.native(userIn.value - monetaryConfig.minerFee - monetaryConfig.minBoxValue),
               minQuoteAmount           = AssetAmount(SigUSD, 0L),
               dexFeePerTokenNum   = 0L,
               dexFeePerTokenDenom = 1L,
               redeemer            = ???
             )
      order = Swap(pool.poolId, monetaryConfig.minerFee, ts, swap, userIn)

      interpreter = new N2TCFMMInterpreter[IO](
                      ExchangeConfig(RecvAddr),
                      monetaryConfig,
                      NetworkContext(curHeight, null)
                    )

      (txc, _) <- interpreter.swap(order, pool)

      uInputs = txc.inputs.map(i => new UnsignedInput(i.boxId))
      utx     = UnsignedErgoLikeTransaction(uInputs, txc.dataInputs, txc.outputCandidates)

      tx = ErgoUnsafeProver.prove(utx, sk)

      _ <- IO.delay(println("TX is:"))
      _ <- IO.delay(println(tx.asJson.spaces2SortKeys))

      _  <- IO.delay(println("Submitting the TX ..."))
      s0 <- IO.delay(submitTx(tx))
      _  <- IO.delay(println(s0.map(id => s"Done. $id")))
    } yield ExitCode.Success
}
