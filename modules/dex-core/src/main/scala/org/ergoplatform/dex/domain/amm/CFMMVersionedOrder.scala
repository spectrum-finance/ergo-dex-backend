package org.ergoplatform.dex.domain.amm

import cats.Show
import derevo.circe.{decoder, encoder}
import derevo.derive
import io.circe.{Decoder, Encoder, Json, ParsingFailure}
import org.ergoplatform.ergo.domain.Output
import tofu.logging.derivation.loggable
import io.circe.syntax._
import io.circe.parser.parse
import cats.syntax.either._
import org.ergoplatform.dex.domain.amm
import tofu.logging.Loggable

sealed trait CFMMVersionedOrder[+V <: CFMMOrderVersion, +O <: CFMMOrderType] {
  val poolId: PoolId
  val box: Output
  val timestamp: Long

  val version: V

  def id: OrderId = OrderId.fromBoxId(box.boxId)
}

object CFMMVersionedOrder {

  implicit val showCfmmAny: Show[CFMMVersionedOrder.Any] = order => s"CFMMVersionedOrder.Any(${order.version})"

  implicit val loggableCfmmAny: Loggable[CFMMVersionedOrder.Any] = Loggable.show

  implicit val encoderAny: Encoder[CFMMVersionedOrder.Any] = {
    case o: SwapV1    => o.asJson
    case o: SwapV0    => o.asJson
    case o: RedeemV1  => o.asJson
    case o: RedeemV0  => o.asJson
    case o: DepositV2 => o.asJson
    case o: DepositV1 => o.asJson
    case o: DepositV0 => o.asJson
  }

  implicit val decoderAny: Decoder[CFMMVersionedOrder.Any] = x =>
    x.as[SwapV1]
      .leftFlatMap(_ => x.as[SwapV0])
      .leftFlatMap(_ => x.as[RedeemV1])
      .leftFlatMap(_ => x.as[RedeemV0])
      .leftFlatMap(_ => x.as[DepositV2])
      .leftFlatMap(_ => x.as[DepositV0])
      .leftFlatMap(_ => x.as[DepositV1])

  type Any = CFMMVersionedOrder[CFMMOrderVersion, CFMMOrderType]

  type AnySwap = CFMMVersionedOrder[CFMMOrderVersion, CFMMOrderType.Swap]

  type AnyRedeem = CFMMVersionedOrder[CFMMOrderVersion, CFMMOrderType.Redeem]

  type AnyDeposit = CFMMVersionedOrder[CFMMOrderVersion, CFMMOrderType.Deposit]

  @derive(encoder, decoder, loggable)
  final case class SwapV1(poolId: PoolId, maxMinerFee: Long, timestamp: Long, params: SwapParams, box: Output)
    extends CFMMVersionedOrder[CFMMOrderVersion.V1, CFMMOrderType.Swap] {
    val version: CFMMOrderVersion.V1 = CFMMOrderVersion.v1
  }

  @derive(encoder, decoder, loggable)
  final case class SwapV0(poolId: PoolId, timestamp: Long, params: SwapParams, box: Output)
    extends CFMMVersionedOrder[CFMMOrderVersion.V0, CFMMOrderType.Swap] {
    val version: CFMMOrderVersion.V0 = CFMMOrderVersion.v0
  }

  @derive(encoder, decoder, loggable)
  final case class DepositV2(poolId: PoolId, maxMinerFee: Long, timestamp: Long, params: DepositParams, box: Output)
    extends CFMMVersionedOrder[CFMMOrderVersion.V2, CFMMOrderType.Deposit] {
    val version: CFMMOrderVersion.V2 = CFMMOrderVersion.v2
  }

  @derive(encoder, decoder, loggable)
  final case class DepositV1(poolId: PoolId, timestamp: Long, params: DepositParams, box: Output)
    extends CFMMVersionedOrder[CFMMOrderVersion.V1, CFMMOrderType.Deposit] {
    val version: CFMMOrderVersion.V1 = CFMMOrderVersion.v1
  }

  final case class DepositV0(poolId: PoolId, timestamp: Long, params: DepositParams, box: Output)
    extends CFMMVersionedOrder[CFMMOrderVersion.V0, CFMMOrderType.Deposit] {
    val version: CFMMOrderVersion.V0 = CFMMOrderVersion.v0
  }

  object DepositV0 {

    implicit val showDepositV0: Show[DepositV0] = order =>
      s"DepositV0(${order.version}, ${order.poolId}, ${order.timestamp}, ${order.params}, ${order.box})"

    implicit val loggableDepositV0: Loggable[DepositV0] = Loggable.show

    implicit val encoderDepositV0: Encoder[DepositV0] = (d: DepositV0) =>
      Json.obj(
        ("poolId", Json.fromString(d.poolId.unwrapped)),
        ("timestamp", Json.fromLong(d.timestamp)),
        ("params", d.params.asJson),
        ("box", d.box.asJson),
        ("version", (d.version: amm.CFMMOrderVersion).asJson)
      )

    implicit val decoder: Decoder[DepositV0] = c =>
      for {
        poolId    <- c.downField("poolId").as[PoolId]
        timestamp <- c.downField("timestamp").as[Long]
        params    <- c.downField("params").as[DepositParams]
        box       <- c.downField("box").as[Output]
        _         <- c.downField("version").as[CFMMOrderVersion]
      } yield DepositV0(poolId, timestamp, params, box)
  }

  @derive(encoder, decoder, loggable)
  final case class RedeemV1(poolId: PoolId, maxMinerFee: Long, timestamp: Long, params: RedeemParams, box: Output)
    extends CFMMVersionedOrder[CFMMOrderVersion.V1, CFMMOrderType.Redeem] {
    val version: CFMMOrderVersion.V1 = CFMMOrderVersion.v1
  }

  @derive(encoder, decoder, loggable)
  final case class RedeemV0(poolId: PoolId, timestamp: Long, params: RedeemParams, box: Output)
    extends CFMMVersionedOrder[CFMMOrderVersion.V0, CFMMOrderType.Redeem] {
    val version: CFMMOrderVersion.V0 = CFMMOrderVersion.v0
  }

}
