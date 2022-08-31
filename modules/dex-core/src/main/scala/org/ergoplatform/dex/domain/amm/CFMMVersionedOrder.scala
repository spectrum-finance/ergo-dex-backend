package org.ergoplatform.dex.domain.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import io.circe.{Decoder, Encoder}
import org.ergoplatform.ergo.domain.Output
import tofu.logging.derivation.loggable
import io.circe.syntax._
import cats.syntax.either._

sealed trait CFMMVersionedOrder[+version <: CFMMOrderVersion, +order <: CFMMOrderType] {
  val poolId: PoolId
  val box: Output
  val timestamp: Long

  def id: OrderId = OrderId.fromBoxId(box.boxId)
}

object CFMMVersionedOrder {

  implicit val encoderAny: Encoder[CFMMVersionedOrder.Any] = {
    case o: SwapV1    => o.asJson
    case o: SwapV0    => o.asJson
    case o: RedeemV1  => o.asJson
    case o: RedeemV0  => o.asJson
    case o: DepositV1 => o.asJson
    case o: DepositV0 => o.asJson
  }

  implicit val decoderAny: Decoder[CFMMVersionedOrder.Any] = x =>
    x.as[SwapV1]
      .leftFlatMap(_ => x.as[SwapV0])
      .leftFlatMap(_ => x.as[RedeemV1])
      .leftFlatMap(_ => x.as[RedeemV0])
      .leftFlatMap(_ => x.as[DepositV1])
      .leftFlatMap(_ => x.as[DepositV0])

  type Any = CFMMVersionedOrder[CFMMOrderVersion, CFMMOrderType]

  type AnySwap = CFMMVersionedOrder[CFMMOrderVersion, CFMMOrderType.Swap]

  type AnyRedeem = CFMMVersionedOrder[CFMMOrderVersion, CFMMOrderType.Redeem]

  type AnyDeposit = CFMMVersionedOrder[CFMMOrderVersion, CFMMOrderType.Deposit]

  @derive(encoder, decoder, loggable)
  final case class SwapV1(poolId: PoolId, maxMinerFee: Long, timestamp: Long, params: SwapParams, box: Output)
    extends CFMMVersionedOrder[CFMMOrderVersion.V1, CFMMOrderType.Swap]

  @derive(encoder, decoder, loggable)
  final case class SwapV0(poolId: PoolId, timestamp: Long, params: SwapParams, box: Output)
    extends CFMMVersionedOrder[CFMMOrderVersion.V0, CFMMOrderType.Swap]

  @derive(encoder, decoder, loggable)
  final case class DepositV1(poolId: PoolId, maxMinerFee: Long, timestamp: Long, params: DepositParams, box: Output)
    extends CFMMVersionedOrder[CFMMOrderVersion.V1, CFMMOrderType.Deposit]

  @derive(encoder, decoder, loggable)
  final case class DepositV0(poolId: PoolId, timestamp: Long, params: DepositParams, box: Output)
    extends CFMMVersionedOrder[CFMMOrderVersion.V0, CFMMOrderType.Deposit]

  @derive(encoder, decoder, loggable)
  final case class RedeemV1(poolId: PoolId, maxMinerFee: Long, timestamp: Long, params: RedeemParams, box: Output)
    extends CFMMVersionedOrder[CFMMOrderVersion.V1, CFMMOrderType.Redeem]

  @derive(encoder, decoder, loggable)
  final case class RedeemV0(poolId: PoolId, timestamp: Long, params: RedeemParams, box: Output)
    extends CFMMVersionedOrder[CFMMOrderVersion.V0, CFMMOrderType.Redeem]

}
