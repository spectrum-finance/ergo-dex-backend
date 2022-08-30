package org.ergoplatform.dex.domain.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.ergo.domain.Output
import tofu.logging.derivation.loggable
import cats.syntax.option._

@derive(encoder, decoder, loggable)
sealed trait CFMMVersionedOrder {
  val poolId: PoolId
  val box: Output
  val timestamp: Long

  def id: OrderId = OrderId.fromBoxId(box.boxId)

  def getMaxMinerFee: Option[Long]
}

object CFMMVersionedOrder {

  @derive(encoder, decoder, loggable)
  sealed trait VersionedSwap extends CFMMVersionedOrder {
    val params: SwapParams

    def setTimestamp(ts: Long): VersionedSwap
  }

  @derive(encoder, decoder, loggable)
  final case class SwapV1(poolId: PoolId, maxMinerFee: Long, timestamp: Long, params: SwapParams, box: Output)
    extends VersionedSwap {
    def setTimestamp(ts: Long): VersionedSwap = this.copy(timestamp = ts)

    def getMaxMinerFee: Option[Long] = maxMinerFee.some
  }

  @derive(encoder, decoder, loggable)
  final case class SwapV0(poolId: PoolId, timestamp: Long, params: SwapParams, box: Output) extends VersionedSwap {
    def setTimestamp(ts: Long): VersionedSwap = this.copy(timestamp = ts)

    def getMaxMinerFee: Option[Long] = none
  }

  @derive(encoder, decoder, loggable)
  sealed trait VersionedDeposit extends CFMMVersionedOrder {
    val params: DepositParams

    def setTimestamp(ts: Long): VersionedDeposit
  }

  @derive(encoder, decoder, loggable)
  final case class DepositV1(poolId: PoolId, maxMinerFee: Long, timestamp: Long, params: DepositParams, box: Output)
    extends VersionedDeposit {
    def setTimestamp(ts: Long): VersionedDeposit = this.copy(timestamp = ts)

    def getMaxMinerFee: Option[Long] = maxMinerFee.some
  }

  @derive(encoder, decoder, loggable)
  final case class DepositV0(poolId: PoolId, timestamp: Long, params: DepositParams, box: Output)
    extends VersionedDeposit {
    def setTimestamp(ts: Long): VersionedDeposit = this.copy(timestamp = ts)

    def getMaxMinerFee: Option[Long] = none

  }

  @derive(encoder, decoder, loggable)
  sealed trait VersionedRedeem extends CFMMVersionedOrder {
    val params: RedeemParams

    def setTimestamp(ts: Long): VersionedRedeem
  }

  @derive(encoder, decoder, loggable)
  final case class RedeemV1(poolId: PoolId, maxMinerFee: Long, timestamp: Long, params: RedeemParams, box: Output)
    extends VersionedRedeem {
    def setTimestamp(ts: Long): VersionedRedeem = this.copy(timestamp = ts)

    def getMaxMinerFee: Option[Long] = maxMinerFee.some
  }

  @derive(encoder, decoder, loggable)
  final case class RedeemV0(poolId: PoolId, timestamp: Long, params: RedeemParams, box: Output)
    extends VersionedRedeem {
    def setTimestamp(ts: Long): VersionedRedeem = this.copy(timestamp = ts)

    def getMaxMinerFee: Option[Long] = none
  }
}
