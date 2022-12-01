package org.ergoplatform.dex.domain.amm

import cats.syntax.either._
import derevo.circe.{decoder, encoder}
import derevo.derive
import io.circe.derivation.{deriveDecoder, deriveEncoder}
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import org.ergoplatform.ergo.domain.Output
import org.ergoplatform.ergo.{PubKey, SErgoTree}
import tofu.logging.derivation.loggable

sealed trait CFMMVersionedOrder[+V <: CFMMOrderVersion, +O <: CFMMOrderType] {
  val poolId: PoolId
  val box: Output
  val timestamp: Long

  val version: V

  def id: OrderId = OrderId.fromBoxId(box.boxId)
}

object CFMMVersionedOrder {

  implicit val encoderAny: Encoder[CFMMVersionedOrder.Any] = {
    case o: SwapV3    => o.asJson
    case o: SwapV2    => o.asJson
    case o: SwapV1    => o.asJson
    case o: SwapV0    => o.asJson
    case o: RedeemV3  => o.asJson
    case o: RedeemV1  => o.asJson
    case o: RedeemV0  => o.asJson
    case o: DepositV3 => o.asJson
    case o: DepositV2 => o.asJson
    case o: DepositV1 => o.asJson
    case o: DepositV0 => o.asJson
  }

  implicit val decoderAny: Decoder[CFMMVersionedOrder.Any] = x =>
    x.as[SwapV3]
      .leftFlatMap(_ => x.as[SwapV2])
      .leftFlatMap(_ => x.as[SwapV1])
      .leftFlatMap(_ => x.as[SwapV0])
      .leftFlatMap(_ => x.as[RedeemV3])
      .leftFlatMap(_ => x.as[RedeemV1])
      .leftFlatMap(_ => x.as[RedeemV0])
      .leftFlatMap(_ => x.as[DepositV3])
      .leftFlatMap(_ => x.as[DepositV2])
      .leftFlatMap(_ => x.as[DepositV0])
      .leftFlatMap(_ => x.as[DepositV1])

  type Any = CFMMVersionedOrder[CFMMOrderVersion, CFMMOrderType]

  type AnySwap = CFMMVersionedOrder[CFMMOrderVersion, CFMMOrderType.SwapType]

  type AnyRedeem = CFMMVersionedOrder[CFMMOrderVersion, CFMMOrderType.RedeemType]

  type AnyDeposit = CFMMVersionedOrder[CFMMOrderVersion, CFMMOrderType.DepositType]

  @derive(encoder, decoder, loggable)
  final case class SwapV3(
    poolId: PoolId,
    maxMinerFee: Long,
    timestamp: Long,
    params: SwapParams[SErgoTree],
    box: Output,
    reservedExFee: Long
  ) extends CFMMVersionedOrder[CFMMOrderVersion.V3, CFMMOrderType.SwapType] {
    val version: CFMMOrderVersion.V3 = CFMMOrderVersion.v3
  }

  @derive(loggable)
  final case class SwapV2(
    poolId: PoolId,
    maxMinerFee: Long,
    timestamp: Long,
    params: SwapParams[SErgoTree],
    box: Output
  ) extends CFMMVersionedOrder[CFMMOrderVersion.V2, CFMMOrderType.SwapType] {
    val version: CFMMOrderVersion.V2 = CFMMOrderVersion.v2
  }

  object SwapV2 {

    implicit val encoderSwapV2: Encoder[SwapV2] =
      deriveEncoder.mapJsonObject(_.add("version", (CFMMOrderVersion.v2: CFMMOrderVersion).asJson))

    implicit val decoderSwapV2: Decoder[SwapV2] =
      deriveDecoder
        .validate(
          _.downField("version")
            .as[CFMMOrderVersion]
            .toOption
            .flatMap {
              case v: CFMMOrderVersion.V2 => Some(v)
              case _                      => None
            }
            .nonEmpty,
          "Incorrect swap v2 version."
        )
  }

  @derive(loggable)
  final case class SwapV1(poolId: PoolId, maxMinerFee: Long, timestamp: Long, params: SwapParams[PubKey], box: Output)
    extends CFMMVersionedOrder[CFMMOrderVersion.V1, CFMMOrderType.SwapType] {
    val version: CFMMOrderVersion.V1 = CFMMOrderVersion.v1
  }

  object SwapV1 {

    implicit val encoderSwapV1: Encoder[SwapV1] =
      deriveEncoder.mapJsonObject(_.add("version", (CFMMOrderVersion.v1: CFMMOrderVersion).asJson))

    implicit val decoderSwapV1: Decoder[SwapV1] =
      deriveDecoder
        .validate(
          _.downField("version")
            .as[CFMMOrderVersion]
            .toOption
            .flatMap {
              case v: CFMMOrderVersion.V1 => Some(v)
              case _                      => None
            }
            .nonEmpty,
          "Incorrect swap v1 version."
        )
  }

  @derive(encoder, decoder, loggable)
  final case class SwapV0(poolId: PoolId, timestamp: Long, params: SwapParams[PubKey], box: Output)
    extends CFMMVersionedOrder[CFMMOrderVersion.V0, CFMMOrderType.SwapType] {
    val version: CFMMOrderVersion.V0 = CFMMOrderVersion.v0
  }

  @derive(loggable)
  final case class DepositV3(
    poolId: PoolId,
    maxMinerFee: Long,
    timestamp: Long,
    params: DepositParams[SErgoTree],
    box: Output
  ) extends CFMMVersionedOrder[CFMMOrderVersion.V3, CFMMOrderType.DepositType] {
    val version: CFMMOrderVersion.V3 = CFMMOrderVersion.v3
  }

  object DepositV3 {

    implicit val encoderDepositV3: Encoder[DepositV3] =
      deriveEncoder.mapJsonObject(_.add("version", (CFMMOrderVersion.v3: CFMMOrderVersion).asJson))

    implicit val decoderDepositV3: Decoder[DepositV3] =
      deriveDecoder
        .validate(
          _.downField("version")
            .as[CFMMOrderVersion]
            .toOption
            .flatMap {
              case v: CFMMOrderVersion.V3 => Some(v)
              case _                      => None
            }
            .nonEmpty,
          "Incorrect deposit v3 version."
        )
  }

  @derive(encoder, decoder, loggable)
  final case class DepositV2(
    poolId: PoolId,
    maxMinerFee: Long,
    timestamp: Long,
    params: DepositParams[PubKey],
    box: Output
  ) extends CFMMVersionedOrder[CFMMOrderVersion.V2, CFMMOrderType.DepositType] {
    val version: CFMMOrderVersion.V2 = CFMMOrderVersion.v2
  }

  @derive(encoder, decoder, loggable)
  final case class DepositV1(poolId: PoolId, timestamp: Long, params: DepositParams[PubKey], box: Output)
    extends CFMMVersionedOrder[CFMMOrderVersion.V1, CFMMOrderType.DepositType] {
    val version: CFMMOrderVersion.V1 = CFMMOrderVersion.v1
  }

  @derive(loggable)
  final case class DepositV0(poolId: PoolId, timestamp: Long, params: DepositParams[PubKey], box: Output)
    extends CFMMVersionedOrder[CFMMOrderVersion.V0, CFMMOrderType.DepositType] {
    val version: CFMMOrderVersion.V0 = CFMMOrderVersion.v0
  }

  object DepositV0 {

    implicit val encoderDepositV0: Encoder[DepositV0] =
      deriveEncoder.mapJsonObject(_.add("version", (CFMMOrderVersion.v0: CFMMOrderVersion).asJson))

    implicit val decoderDepositV0: Decoder[DepositV0] =
      deriveDecoder
        .validate(
          _.downField("version")
            .as[CFMMOrderVersion]
            .toOption
            .flatMap {
              case v: CFMMOrderVersion.V0 => Some(v)
              case _                      => None
            }
            .nonEmpty,
          "Incorrect deposit v0 version."
        )
  }

  @derive(loggable)
  final case class RedeemV3(
    poolId: PoolId,
    maxMinerFee: Long,
    timestamp: Long,
    params: RedeemParams[SErgoTree],
    box: Output
  ) extends CFMMVersionedOrder[CFMMOrderVersion.V3, CFMMOrderType.RedeemType] {
    val version: CFMMOrderVersion.V3 = CFMMOrderVersion.v3
  }

  object RedeemV3 {

    implicit val encoderRedeemV3: Encoder[RedeemV3] =
      deriveEncoder.mapJsonObject(_.add("version", (CFMMOrderVersion.v3: CFMMOrderVersion).asJson))

    implicit val decoderRedeemV3: Decoder[RedeemV3] =
      deriveDecoder
        .validate(
          _.downField("version")
            .as[CFMMOrderVersion]
            .toOption
            .flatMap {
              case v: CFMMOrderVersion.V3 => Some(v)
              case _                      => None
            }
            .nonEmpty,
          "Incorrect redeem v3 version."
        )
  }

  @derive(encoder, decoder, loggable)
  final case class RedeemV1(
    poolId: PoolId,
    maxMinerFee: Long,
    timestamp: Long,
    params: RedeemParams[PubKey],
    box: Output
  ) extends CFMMVersionedOrder[CFMMOrderVersion.V1, CFMMOrderType.RedeemType] {
    val version: CFMMOrderVersion.V1 = CFMMOrderVersion.v1
  }

  @derive(encoder, decoder, loggable)
  final case class RedeemV0(poolId: PoolId, timestamp: Long, params: RedeemParams[PubKey], box: Output)
    extends CFMMVersionedOrder[CFMMOrderVersion.V0, CFMMOrderType.RedeemType] {
    val version: CFMMOrderVersion.V0 = CFMMOrderVersion.v0
  }

}
