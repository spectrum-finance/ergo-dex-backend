package org.ergoplatform.dex.markets.api.v1.models.amm

import derevo.cats.show
import derevo.circe.encoder
import derevo.derive
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.ergo.TxId
import sttp.tapir.Schema
import tofu.logging.derivation.loggable

@derive(show, encoder, loggable)
sealed trait Order {
  val timestamp: Long
  val registerTransactionId: TxId
  val status: OrderStatus
}

object Order {

  @derive(show, encoder, loggable)
  case class Swap(
    baseAmount: AssetAmount,
    timestamp: Long,
    registerTransactionId: TxId,
    poolId: PoolId,
    fee: AssetAmount,
    quoteAmount: Option[AssetAmount],
    executedTransactionId: Option[TxId],
    status: OrderStatus
  ) extends Order

  @derive(show, encoder, loggable)
  case class Deposit(
    x: AssetAmount,
    y: AssetAmount,
    timestamp: Long,
    registerTransactionId: TxId,
    poolId: PoolId,
    fee: AssetAmount,
    executedTransactionId: Option[TxId],
    lpReward: Option[AssetAmount],
    status: OrderStatus
  ) extends Order

  @derive(show, encoder, loggable)
  case class Redeem(
    lp: AssetAmount,
    timestamp: Long,
    registerTransactionId: TxId,
    poolId: PoolId,
    fee: AssetAmount,
    executedTransactionId: Option[TxId],
    x: Option[AssetAmount],
    y: Option[AssetAmount],
    status: OrderStatus
  ) extends Order

  @derive(show, encoder, loggable)
  case class Lock(
    lockedAmount: AssetAmount,
    timestamp: Long,
    registerTransactionId: TxId,
    deadline: Long,
    fee: AssetAmount,
    status: OrderStatus
  ) extends Order

  @derive(show, encoder, loggable)
  case class AnyOrder(
    lockedAmount: AssetAmount,
    timestamp: Long,
    registerTransactionId: TxId,
    deadline: Long,
    fee: AssetAmount,
    status: OrderStatus
  ) extends Order

  implicit val schema: Schema[Order] = Schema.derived
}
