package org.ergoplatform.dex.index.db

import doobie.util.Write
import io.circe.Json
import org.ergoplatform.dex.domain.amm.{Deposit, PoolId, Redeem, Swap}
import org.ergoplatform.ergo.models.{BoxAsset, Output, RegisterId, SConstant}
import org.ergoplatform.ergo.{Address, BoxId, SErgoTree, TokenId, TxId}
import instances._
import io.circe.syntax._
import shapeless.Lazy

object models {

  case class DBSwap(
    poolId: PoolId,
    maxMinerFee: Long,
    timestamp: Long,
    input_id: TokenId,
    input_value: Long,
    input_ticker: Option[String],
    min_output_id: TokenId,
    min_output_value: Long,
    min_output_ticker: Option[String],
    dexFeePerTokenNum: Long,
    dexFeePerTokenDenom: Long,
    p2pk: Address
  )

  object DBSwap {

    implicit def write: Write[DBSwap] = Lazy(implicitly[Write[DBSwap]]).value

  }

  def swapToDb(swap: Swap): DBSwap =
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

  case class DBRedeem(
    poolId: PoolId,
    maxMinerFee: Long,
    timestamp: Long,
    lp_id: TokenId,
    lp_value: Long,
    lp_ticker: Option[String],
    dexFee: Long,
    p2pk: Address
  )

  object DBRedeem {

    implicit def write: Write[DBRedeem] = Lazy(implicitly[Write[DBRedeem]]).value

  }

  def redeemToDb(redeem: Redeem): DBRedeem =
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

  case class DBDeposit(
    poolId: PoolId,
    maxMinerFee: Long,
    timestamp: Long,
    in_x_id: TokenId,
    in_x_value: Long,
    in_x_ticker: Option[String],
    in_y_id: TokenId,
    in_y_value: Long,
    in_y_ticker: Option[String],
    dexFee: Long,
    p2pk: Address
  )

  object DBDeposit {

    implicit def write: Write[DBDeposit] = Lazy(implicitly[Write[DBDeposit]]).value

  }

  def depositToDb(deposit: Deposit): DBDeposit =
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

  object DBOutput {

    implicit def write: Write[DBOutput] = Lazy(implicitly[Write[DBOutput]]).value

  }

  def outputToDb(output: Output): DBOutput =
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
