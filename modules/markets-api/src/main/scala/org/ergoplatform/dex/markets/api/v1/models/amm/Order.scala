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
  val timestamp: Option[Long]
  val registerTransactionId: Option[TxId]
  val fee: Option[Long]
  val status: Option[OrderStatus]
}

object Order {

  @derive(show, encoder, loggable)
  case class Swap(
    baseAmount: AssetAmount,
    timestamp: Option[Long],
    registerTransactionId: Option[TxId],
    poolId: PoolId,
    fee: Option[Long],
    quoteAmount: Option[AssetAmount],
    executedTransactionId: Option[TxId],
    status: Option[OrderStatus]
  ) extends Order

  @derive(show, encoder, loggable)
  case class Deposit(
    x: AssetAmount,
    y: AssetAmount,
    timestamp: Option[Long],
    registerTransactionId: Option[TxId],
    poolId: PoolId,
    fee: Option[Long],
    executedTransactionId: Option[TxId],
    lpReward: Option[AssetAmount],
    status: Option[OrderStatus]
  ) extends Order

  @derive(show, encoder, loggable)
  case class Redeem(
    lp: AssetAmount,
    timestamp: Option[Long],
    registerTransactionId: Option[TxId],
    poolId: PoolId,
    fee: Option[Long],
    executedTransactionId: Option[TxId],
    x: Option[AssetAmount],
    y: Option[AssetAmount],
    status: Option[OrderStatus]
  ) extends Order

  @derive(show, encoder, loggable)
  case class Lock(
    lockedAmount: AssetAmount,
    timestamp: Option[Long],
    registerTransactionId: Option[TxId],
    deadline: Long,
    fee: Option[Long],
    status: Option[OrderStatus]
  ) extends Order

  @derive(show, encoder, loggable)
  case class AnyOrder(
    x: Option[AssetAmount],
    y: Option[AssetAmount],
    lp: Option[AssetAmount],
    lockedAmount: Option[AssetAmount],
    deadline: Option[Long],
    timestamp: Option[Long],
    registerTransactionId: Option[TxId],
    poolId: Option[PoolId],
    fee: Option[Long],
    executedTransactionId: Option[TxId],
    status: Option[OrderStatus]
  ) extends Order

  implicit val schema: Schema[Order] = Schema.derived
}
