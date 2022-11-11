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
    case o: SwapMultiAddress => o.asJson
    case o: SwapP2Pk         => o.asJson
    case o: SwapV0           => o.asJson
    case o: RedeemV1         => o.asJson
    case o: RedeemV0         => o.asJson
    case o: DepositV2        => o.asJson
    case o: DepositV1        => o.asJson
    case o: DepositV0        => o.asJson
  }

  implicit val decoderAny: Decoder[CFMMVersionedOrder.Any] = x =>
    x.as[SwapP2Pk]
      .leftFlatMap(_ => x.as[SwapMultiAddress])
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

  @derive(loggable)
  final case class SwapP2Pk(poolId: PoolId, maxMinerFee: Long, timestamp: Long, params: SwapParams[PubKey], box: Output)
    extends CFMMVersionedOrder[CFMMOrderVersion.V1, CFMMOrderType.Swap] {
    val version: CFMMOrderVersion.V1 = CFMMOrderVersion.v1
  }

  object SwapP2Pk {

    implicit val encoderSwapP2Pk: Encoder[SwapP2Pk] =
      deriveEncoder.mapJsonObject(_.add("version", (CFMMOrderVersion.v1: CFMMOrderVersion).asJson))

    implicit val decoderSwapP2Pk: Decoder[SwapP2Pk] =
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

  @derive(loggable)
  final case class SwapMultiAddress(
    poolId: PoolId,
    maxMinerFee: Long,
    timestamp: Long,
    params: SwapParams[SErgoTree],
    box: Output
  ) extends CFMMVersionedOrder[CFMMOrderVersion.V2, CFMMOrderType.Swap] {
    val version: CFMMOrderVersion.V2 = CFMMOrderVersion.v2
  }

  object SwapMultiAddress {

    implicit val encoderSwapMultiAddress: Encoder[SwapMultiAddress] =
      deriveEncoder.mapJsonObject(_.add("version", (CFMMOrderVersion.v2: CFMMOrderVersion).asJson))

    implicit val decoderSwapMultiAddress: Decoder[SwapMultiAddress] =
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

  @derive(encoder, decoder, loggable)
  final case class SwapV0(poolId: PoolId, timestamp: Long, params: SwapParams[PubKey], box: Output)
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

  @derive(loggable)
  final case class DepositV0(poolId: PoolId, timestamp: Long, params: DepositParams, box: Output)
    extends CFMMVersionedOrder[CFMMOrderVersion.V0, CFMMOrderType.Deposit] {
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
