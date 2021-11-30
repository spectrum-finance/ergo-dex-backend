package org.ergoplatform.dex.index.db

import io.circe.Json
import io.circe.syntax._
import org.ergoplatform.dex.domain.amm.{Deposit, PoolId, Redeem, Swap}
import org.ergoplatform.ergo.models.Output
import org.ergoplatform.ergo._

object models {

  final case class DBSwap(
    poolId: PoolId,
    maxMinerFee: Long,
    timestamp: Long,
    inputId: TokenId,
    inputValue: Long,
    inputTicker: Option[String],
    minOutputId: TokenId,
    minOutputValue: Long,
    minOutputTicker: Option[String],
    dexFeePerTokenNum: Long,
    dexFeePerTokenDenom: Long,
    p2pk: Address
  )

  implicit val swapView: DBView[Swap, DBSwap] =
    (swap: Swap) =>
      DBSwap(
        swap.poolId,
        swap.maxMinerFee,
        swap.timestamp,
        swap.params.input.id,
        swap.params.input.value,
        swap.params.input.ticker,
        swap.params.minOutput.id,
        swap.params.minOutput.value,
        swap.params.minOutput.ticker,
        swap.params.dexFeePerTokenNum,
        swap.params.dexFeePerTokenDenom,
        swap.params.p2pk
      )

  final case class DBRedeem(
    poolId: PoolId,
    maxMinerFee: Long,
    timestamp: Long,
    lpId: TokenId,
    lpValue: Long,
    lpTicker: Option[String],
    dexFee: Long,
    p2pk: Address
  )

  implicit val redeemView: DBView[Redeem, DBRedeem] =
    (redeem: Redeem) =>
      DBRedeem(
        redeem.poolId,
        redeem.maxMinerFee,
        redeem.timestamp,
        redeem.params.lp.id,
        redeem.params.lp.value,
        redeem.params.lp.ticker,
        redeem.params.dexFee,
        redeem.params.p2pk
      )

  final case class DBDeposit(
    poolId: PoolId,
    maxMinerFee: Long,
    timestamp: Long,
    inputIdX: TokenId,
    inputValueX: Long,
    inputTickerX: Option[String],
    inputIdY: TokenId,
    inputValueY: Long,
    inputTickerY: Option[String],
    dexFee: Long,
    p2pk: Address
  )

  implicit val depositView: DBView[Deposit, DBDeposit] =
    (deposit: Deposit) =>
      DBDeposit(
        deposit.poolId,
        deposit.maxMinerFee,
        deposit.timestamp,
        deposit.params.inX.id,
        deposit.params.inX.value,
        deposit.params.inX.ticker,
        deposit.params.inY.id,
        deposit.params.inY.value,
        deposit.params.inY.ticker,
        deposit.params.dexFee,
        deposit.params.p2pk
      )

  final case class DBOutput(
    boxId: BoxId,
    transactionId: TxId,
    value: Long,
    index: Int,
    globalIndex: Long,
    creationHeight: Int,
    settlementHeight: Int,
    ergoTree: SErgoTree,
    address: Address,
    additionalRegisters: Json
  )

  implicit val outputView: DBView[Output, DBOutput] =
    (output: Output) =>
      DBOutput(
        output.boxId,
        output.transactionId,
        output.value,
        output.index,
        output.globalIndex,
        output.creationHeight,
        output.settlementHeight,
        output.ergoTree,
        output.address,
        output.additionalRegisters.asJson
      )
}
