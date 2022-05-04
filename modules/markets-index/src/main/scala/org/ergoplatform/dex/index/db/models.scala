package org.ergoplatform.dex.index.db

import org.ergoplatform.common.sql.QuerySet
import org.ergoplatform.dex.domain.Ticker
import org.ergoplatform.dex.domain.amm.OrderEvaluation.{DepositEvaluation, RedeemEvaluation, SwapEvaluation}
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.domain.locks.LiquidityLock
import org.ergoplatform.dex.domain.locks.types.LockId
import org.ergoplatform.dex.index.sql._
import org.ergoplatform.ergo._
import org.ergoplatform.ergo.domain.{LedgerMetadata, Block}
import org.ergoplatform.ergo.services.explorer.models.TokenInfo
import org.ergoplatform.ergo.state.ConfirmedIndexed

object models {

  final case class DBBlock(
    id: String,
    height: Int,
    timestamp: Long
  )

  implicit val blockQs: QuerySet[DBBlock] = BlocksSql

  implicit val dbBlockView: Extract[Block, DBBlock] = {
    case Block(id, height, timestamp) =>
      DBBlock(id, height, timestamp)
  }

  final case class DBLiquidityLock(
    id: LockId,
    deadline: Int,
    tokenId: TokenId,
    amount: Long,
    redeemer: Address
  )

  implicit val lqLockQs: QuerySet[DBLiquidityLock] = LqLocksSql

  implicit val lqLockView: Extract[LiquidityLock, DBLiquidityLock] = {
    case LiquidityLock(id, deadline, amount, redeemer) =>
      DBLiquidityLock(id, deadline, amount.id, amount.value, redeemer)
  }

  final case class DBPoolSnapshot(
    stateId: PoolStateId,
    poolId: PoolId,
    lpId: TokenId,
    lpAmount: Long,
    xId: TokenId,
    xAmount: Long,
    yId: TokenId,
    yAmount: Long,
    feeNum: Int,
    globalIndex: Long,
    settlementHeight: Int,
    protocolVersion: ProtocolVersion
  )

  implicit val poolQs: QuerySet[DBPoolSnapshot] = CFMMPoolSql

  implicit val poolView: Extract[ConfirmedIndexed[CFMMPool], DBPoolSnapshot] = {
    case ConfirmedIndexed(pool, LedgerMetadata(gix, height)) =>
      DBPoolSnapshot(
        PoolStateId.fromBoxId(pool.box.boxId),
        pool.poolId,
        pool.lp.id,
        pool.lp.value,
        pool.x.id,
        pool.x.value,
        pool.y.id,
        pool.y.value,
        pool.feeNum,
        gix,
        height,
        ProtocolVersion.Initial
      )
  }

  final case class DBSwap(
    orderId: OrderId,
    poolId: PoolId,
    poolStateId: Option[PoolStateId],
    maxMinerFee: Long,
    timestamp: Long,
    inputId: TokenId,
    inputValue: Long,
    minOutputId: TokenId,
    minOutputAmount: Long,
    outputAmount: Option[Long],
    dexFeePerTokenNum: Long,
    dexFeePerTokenDenom: Long,
    redeemer: PubKey,
    protocolVersion: ProtocolVersion
  )

  implicit val swapQs: QuerySet[DBSwap] = SwapOrdersSql

  implicit val swapView: Extract[EvaluatedCFMMOrder[Swap, SwapEvaluation], DBSwap] = {
    case EvaluatedCFMMOrder(swap, ev, pool) =>
      DBSwap(
        OrderId.fromBoxId(swap.box.boxId),
        swap.poolId,
        pool.map(p => PoolStateId(p.box.boxId)),
        swap.maxMinerFee,
        swap.timestamp,
        swap.params.input.id,
        swap.params.input.value,
        swap.params.minOutput.id,
        swap.params.minOutput.value,
        ev.map(_.output.value),
        swap.params.dexFeePerTokenNum,
        swap.params.dexFeePerTokenDenom,
        swap.params.redeemer,
        ProtocolVersion.Initial
      )
  }

  final case class DBRedeem(
    orderId: OrderId,
    poolId: PoolId,
    poolStateId: Option[PoolStateId],
    maxMinerFee: Long,
    timestamp: Long,
    lpId: TokenId,
    lpAmount: Long,
    outputAmountX: Option[Long],
    outputAmountY: Option[Long],
    dexFee: Long,
    redeemer: PubKey,
    protocolVersion: ProtocolVersion
  )

  implicit val redeemQs: QuerySet[DBRedeem] = RedeemOrdersSql

  implicit val redeemView: Extract[EvaluatedCFMMOrder[Redeem, RedeemEvaluation], DBRedeem] = {
    case EvaluatedCFMMOrder(redeem, ev, pool) =>
      DBRedeem(
        OrderId.fromBoxId(redeem.box.boxId),
        redeem.poolId,
        pool.map(p => PoolStateId(p.box.boxId)),
        redeem.maxMinerFee,
        redeem.timestamp,
        redeem.params.lp.id,
        redeem.params.lp.value,
        ev.map(_.outputX.value),
        ev.map(_.outputY.value),
        redeem.params.dexFee,
        redeem.params.redeemer,
        ProtocolVersion.Initial
      )
  }

  final case class DBDeposit(
    orderId: OrderId,
    poolId: PoolId,
    poolStateId: Option[PoolStateId],
    maxMinerFee: Long,
    timestamp: Long,
    inputIdX: TokenId,
    inputAmountX: Long,
    inputIdY: TokenId,
    inputAmountY: Long,
    outputAmountLP: Option[Long],
    dexFee: Long,
    redeemer: PubKey,
    protocolVersion: ProtocolVersion
  )

  implicit val depositQs: QuerySet[DBDeposit] = DepositOrdersSql

  implicit val depositView: Extract[EvaluatedCFMMOrder[Deposit, DepositEvaluation], DBDeposit] = {
    case EvaluatedCFMMOrder(deposit, ev, pool) =>
      DBDeposit(
        OrderId.fromBoxId(deposit.box.boxId),
        deposit.poolId,
        pool.map(p => PoolStateId(p.box.boxId)),
        deposit.maxMinerFee,
        deposit.timestamp,
        deposit.params.inX.id,
        deposit.params.inX.value,
        deposit.params.inY.id,
        deposit.params.inY.value,
        ev.map(_.outputLP.value),
        deposit.params.dexFee,
        deposit.params.redeemer,
        ProtocolVersion.Initial
      )
  }

  final case class DBAssetInfo(
    tokenId: TokenId,
    ticker: Option[Ticker],
    decimals: Option[Int]
  )

  implicit val extractAssets: Extract[TokenInfo, DBAssetInfo] =
    ti => DBAssetInfo(ti.id, ti.name.map(Ticker.apply), ti.decimals)
}
