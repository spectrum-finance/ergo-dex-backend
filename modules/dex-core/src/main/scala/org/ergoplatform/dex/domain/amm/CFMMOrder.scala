package org.ergoplatform.dex.domain.amm

import cats.Show
import cats.syntax.either._
import cats.syntax.show._
import derevo.cats.show
import derevo.circe.{codec, decoder, encoder}
import derevo.derive
import io.circe.derivation.{deriveDecoder, deriveEncoder}
import io.circe.syntax._
import io.circe.{Codec, Decoder, Encoder}
import org.ergoplatform.dex.domain.amm.CFMMOrderType.{DepositType, RedeemType, SwapType}
import org.ergoplatform.ergo.domain.Output
import org.ergoplatform.ergo.{PubKey, SErgoTree, TokenId}
import tofu.logging.Loggable
import tofu.logging.derivation.loggable

sealed trait CFMMOrder[+O <: CFMMOrderType] {
  val poolId: PoolId
  val box: Output
  val timestamp: Long

  val orderType: O

  def id: OrderId = OrderId.fromBoxId(box.boxId)
}

object CFMMOrder {

  type Any        = CFMMOrder[CFMMOrderType]
  type SwapAny    = CFMMOrder[CFMMOrderType.SwapType]
  type DepositAny = CFMMOrder[CFMMOrderType.DepositType]
  type RedeemAny  = CFMMOrder[CFMMOrderType.RedeemType]

  implicit def decoderAny: Decoder[Any] = x =>
    x.as[DepositTokenFee]
      .leftFlatMap(_ => x.as[DepositErgFee])
      .leftFlatMap(_ => x.as[RedeemTokenFee])
      .leftFlatMap(_ => x.as[RedeemErgFee])
      .leftFlatMap(_ => x.as[SwapTokenFee])
      .leftFlatMap(_ => x.as[SwapP2Pk])
      .leftFlatMap(_ => x.as[SwapMultiAddress])

  implicit def encoderAny: Encoder[Any] = Encoder.instance {
    case value: DepositErgFee    => value.asJson
    case value: DepositTokenFee  => value.asJson
    case value: RedeemErgFee     => value.asJson
    case value: RedeemTokenFee   => value.asJson
    case value: SwapTokenFee     => value.asJson
    case value: SwapP2Pk         => value.asJson
    case value: SwapMultiAddress => value.asJson
  }

  implicit val showAny: Show[Any] = {
    case d: DepositErgFee    => d.show
    case d: DepositTokenFee  => d.show
    case r: RedeemTokenFee   => r.show
    case r: RedeemErgFee     => r.show
    case s: SwapTokenFee     => s.show
    case s: SwapP2Pk         => s.show
    case s: SwapMultiAddress => s.show
  }

  implicit val showSwapAny: Show[SwapAny] = {
    case s: SwapP2Pk         => s.show
    case s: SwapMultiAddress => s.show
    case s: SwapTokenFee     => s.show
  }

  implicit val loggableCFMMOrder: Loggable[CFMMOrder.Any] = Loggable.show

  implicit val loggableSwapAny: Loggable[SwapAny] = Loggable.show

  @derive(show, loggable, encoder, decoder, codec)
  final case class DepositErgFee(poolId: PoolId, maxMinerFee: Long, timestamp: Long, params: DepositParams, box: Output)
    extends CFMMOrder[DepositType.DepositErgFee] {
    val orderType: DepositType.DepositErgFee = DepositType.depositErgFee
  }

  @derive(show, loggable, encoder, decoder, codec)
  final case class DepositTokenFee(
    poolId: PoolId,
    maxMinerFee: Long,
    timestamp: Long,
    params: DepositParams,
    box: Output,
    depositFee: DepositFee
  ) extends CFMMOrder[DepositType.DepositTokenFee] {
    val orderType: DepositType.DepositTokenFee = DepositType.depositTokenFee
  }

  @derive(show, loggable, encoder, decoder, codec)
  final case class RedeemErgFee(poolId: PoolId, maxMinerFee: Long, timestamp: Long, params: RedeemParams, box: Output)
    extends CFMMOrder[RedeemType.RedeemErgFee] {
    val orderType: RedeemType.RedeemErgFee = RedeemType.redeemErgFee
  }

  @derive(show, loggable, encoder, decoder, codec)
  final case class RedeemTokenFee(
    poolId: PoolId,
    maxMinerFee: Long,
    timestamp: Long,
    params: RedeemParams,
    box: Output,
    spectrumTokenFee: Long
  ) extends CFMMOrder[RedeemType.RedeemTokenFee] {
    val orderType: RedeemType.RedeemTokenFee = RedeemType.redeemTokenFee
  }

  @derive(show, loggable)
  final case class SwapP2Pk(
    poolId: PoolId,
    maxMinerFee: Long,
    timestamp: Long,
    params: SwapParams[PubKey],
    box: Output
  ) extends CFMMOrder[SwapType.SwapP2Pk] {
    val orderType: SwapType.SwapP2Pk = SwapType.swapP2Pk
  }

  object SwapP2Pk {

    implicit def codecSwapP2Pk: Codec.AsObject[SwapP2Pk] =
      Codec.AsObject.from(decoderSwapP2Pk, encoderSwapP2Pk)

    implicit def encoderSwapP2Pk: Encoder.AsObject[SwapP2Pk] =
      deriveEncoder.mapJsonObject(_.add("orderType", SwapType.swapP2Pk.asJson))

    implicit def decoderSwapP2Pk: Decoder[SwapP2Pk] =
      deriveDecoder
        .validate(
          _.downField("orderType").as[SwapType.SwapP2Pk].toOption.nonEmpty,
          "Incorrect swap p2pk order. There is no proper order type."
        )
  }

  @derive(show, loggable)
  final case class SwapMultiAddress(
    poolId: PoolId,
    maxMinerFee: Long,
    timestamp: Long,
    params: SwapParams[SErgoTree],
    box: Output
  ) extends CFMMOrder[SwapType.SwapMultiAddress] {
    val orderType: SwapType.SwapMultiAddress = SwapType.swapMultiAddress
  }

  object SwapMultiAddress {

    implicit def codecSwapMultiAddress: Codec.AsObject[SwapMultiAddress] =
      Codec.AsObject.from(decoderSwapMultiAddress, encoderSwapMultiAddress)

    implicit def encoderSwapMultiAddress: Encoder.AsObject[SwapMultiAddress] =
      deriveEncoder.mapJsonObject(_.add("orderType", SwapType.swapMultiAddress.asJson))

    implicit def decoderSwapMultiAddress: Decoder[SwapMultiAddress] =
      deriveDecoder
        .validate(
          _.downField("orderType").as[SwapType.SwapMultiAddress].toOption.nonEmpty,
          "Incorrect swap multi address order. There is no proper order type."
        )
  }

  @derive(show, loggable, encoder, decoder, codec)
  final case class SwapTokenFee(
    poolId: PoolId,
    maxMinerFee: Long,
    timestamp: Long,
    params: SwapParams[SErgoTree],
    box: Output,
    feeTokenId: TokenId,
    reservedExFee: Long //todo validate base + reserve == actual
  ) extends CFMMOrder[SwapType.SwapTokenFee] {
    val orderType: SwapType.SwapTokenFee = SwapType.swapTokenFee
  }
}
