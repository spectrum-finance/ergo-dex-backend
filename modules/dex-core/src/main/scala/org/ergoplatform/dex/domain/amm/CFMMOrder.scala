package org.ergoplatform.dex.domain.amm

import cats.Show
import derevo.cats.show
import derevo.circe.{decoder, encoder}
import derevo.derive
import io.circe.derivation.{deriveCodec, deriveDecoder, deriveEncoder}
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import org.ergoplatform.dex.domain.amm.CFMMOrderType.{DepositType, RedeemType, SwapType}
import org.ergoplatform.ergo.domain.Output
import org.ergoplatform.ergo.{PubKey, SErgoTree}
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

  type AnyOrder   = CFMMOrder[CFMMOrderType]
  type AnySwap    = CFMMOrder[SwapType]
  type AnyDeposit = CFMMOrder[DepositType]
  type AnyRedeem  = CFMMOrder[RedeemType]

  implicit def decoderOrder: Decoder[CFMMOrder[CFMMOrderType]] = deriveDecoder
  implicit def encoderOrder: Encoder[CFMMOrder[CFMMOrderType]] = deriveEncoder

  @derive(loggable)
  final case class DepositErgFee(
    poolId: PoolId,
    maxMinerFee: Long,
    timestamp: Long,
    params: DepositParams[PubKey],
    box: Output
  ) extends CFMMOrder[DepositType] {
    val orderType: DepositType = DepositType.depositErgFee
  }

  object DepositErgFee {

    implicit val encoder: Encoder.AsObject[DepositErgFee] =
      deriveEncoder.mapJsonObject(_.add("orderType", DepositType.depositErgFee.asJson))

    implicit val decoder: Decoder[DepositErgFee] =
      deriveDecoder
        .validate(
          _.downField("orderType").as[DepositType.DepositErgFee].toOption.nonEmpty,
          "Incorrect DepositErgFee order. There is no order type in json."
        )
  }

  @derive(loggable)
  final case class DepositTokenFee(
    poolId: PoolId,
    maxMinerFee: Long,
    timestamp: Long,
    params: DepositParams[SErgoTree],
    box: Output
  ) extends CFMMOrder[DepositType] {
    val orderType: DepositType = DepositType.depositTokenFee
  }

  object DepositTokenFee {

    implicit val encoder: Encoder.AsObject[DepositTokenFee] =
      deriveEncoder.mapJsonObject(_.add("orderType", DepositType.depositTokenFee.asJson))

    implicit val decoder: Decoder[DepositTokenFee] =
      deriveDecoder
        .validate(
          _.downField("orderType").as[DepositType.DepositTokenFee].toOption.nonEmpty,
          "Incorrect DepositTokenFee order. There is no order type in json."
        )
  }

  @derive(loggable)
  final case class RedeemErgFee(
    poolId: PoolId,
    maxMinerFee: Long,
    timestamp: Long,
    params: RedeemParams[PubKey],
    box: Output
  ) extends CFMMOrder[RedeemType] {
    val orderType: RedeemType = RedeemType.redeemErgFee
  }

  object RedeemErgFee {

    implicit val encoder: Encoder.AsObject[RedeemErgFee] =
      deriveEncoder.mapJsonObject(_.add("orderType", RedeemType.redeemErgFee.asJson))

    implicit val decoder: Decoder[RedeemErgFee] =
      deriveDecoder
        .validate(
          _.downField("orderType").as[RedeemType.RedeemErgFee].toOption.nonEmpty,
          "Incorrect RedeemErgFee order. There is no order type in json."
        )
  }

  @derive(loggable)
  final case class RedeemTokenFee(
    poolId: PoolId,
    maxMinerFee: Long,
    timestamp: Long,
    params: RedeemParams[SErgoTree],
    box: Output
  ) extends CFMMOrder[RedeemType] {
    val orderType: RedeemType = RedeemType.redeemTokenFee
  }

  object RedeemTokenFee {

    implicit val encoder: Encoder.AsObject[RedeemTokenFee] =
      deriveEncoder.mapJsonObject(_.add("orderType", RedeemType.redeemTokenFee.asJson))

    implicit val decoder: Decoder[RedeemTokenFee] =
      deriveDecoder
        .validate(
          _.downField("orderType").as[RedeemType.RedeemTokenFee].toOption.nonEmpty,
          "Incorrect RedeemTokenFee order. There is no order type in json."
        )
  }

  @derive(loggable, encoder, decoder)
  sealed trait SwapErg extends CFMMOrder[SwapType]

  @derive(loggable)
  final case class SwapP2Pk(
    poolId: PoolId,
    maxMinerFee: Long,
    timestamp: Long,
    params: SwapParams[PubKey],
    box: Output
  ) extends SwapErg {
    val orderType: SwapType = SwapType.swapP2Pk
  }

  object SwapP2Pk {

    implicit val encoder: Encoder.AsObject[SwapP2Pk] =
      deriveEncoder.mapJsonObject(_.add("orderType", SwapType.swapP2Pk.asJson))

    implicit val decoder: Decoder[SwapP2Pk] =
      deriveDecoder
        .validate(
          _.downField("orderType").as[SwapType.SwapP2Pk].toOption.nonEmpty,
          "Incorrect SwapP2Pk order. There is no order type in json."
        )
  }

  @derive(loggable)
  final case class SwapMultiAddress(
    poolId: PoolId,
    maxMinerFee: Long,
    timestamp: Long,
    params: SwapParams[SErgoTree],
    box: Output
  ) extends SwapErg {
    val orderType: SwapType = SwapType.swapMultiAddress
  }

  object SwapMultiAddress {

    implicit val encoder: Encoder.AsObject[SwapMultiAddress] =
      deriveEncoder.mapJsonObject(_.add("orderType", SwapType.swapMultiAddress.asJson))

    implicit val decoder: Decoder[SwapMultiAddress] =
      deriveDecoder
        .validate(
          _.downField("orderType").as[SwapType.SwapMultiAddress].toOption.nonEmpty,
          "Incorrect SwapMultiAddress order. There is no order type in json."
        )
  }

  @derive(show, loggable, encoder, decoder)
  final case class SwapTokenFee(
    poolId: PoolId,
    maxMinerFee: Long,
    timestamp: Long,
    params: SwapParams[SErgoTree],
    box: Output,
    reservedExFee: Long
  ) extends CFMMOrder[SwapType.SwapTokenFee] {
    val orderType: SwapType.SwapTokenFee = SwapType.swapTokenFee
  }

  implicit def showAny: Show[AnyOrder] = _.toString

  implicit def loggableAny: Loggable[CFMMOrder.AnyOrder] = Loggable.show

  implicit def showAnySwap: Show[AnySwap] = _.toString

  implicit def loggableAnySwap: Loggable[CFMMOrder.AnySwap] = Loggable.show

  implicit def showAnyDeposit: Show[AnyDeposit] = _.toString

  implicit def loggableAnyDeposit: Loggable[CFMMOrder.AnyDeposit] = Loggable.show

  implicit def showAnyRedeem: Show[AnyRedeem] = _.toString

  implicit def loggableAnyRedeem: Loggable[CFMMOrder.AnyRedeem] = Loggable.show
}
