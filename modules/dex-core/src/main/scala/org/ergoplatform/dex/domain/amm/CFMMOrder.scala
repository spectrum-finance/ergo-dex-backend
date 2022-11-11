package org.ergoplatform.dex.domain.amm

import cats.Show
import derevo.circe.{decoder, encoder}
import derevo.derive
import io.circe.derivation.{deriveDecoder, deriveEncoder}
import io.circe.syntax._
import cats.syntax.either._
import cats.syntax.show._
import derevo.cats.show
import io.circe.{Decoder, Encoder}
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

  type Any     = CFMMOrder[CFMMOrderType.Any]
  type SwapAny = CFMMOrder[CFMMOrderType.Swap]

  implicit val encoderAny: Encoder[CFMMOrder.Any] = {
    case deposit: Deposit            => deposit.asJson
    case redeem: Redeem              => redeem.asJson
    case swap: Swap                  => swap.asJson
    case swapMulti: SwapMultiAddress => swapMulti.asJson
  }

  implicit val decoderAny: Decoder[CFMMOrder.Any] = x =>
    x.as[Deposit]
      .leftFlatMap(_ => x.as[Redeem])
      .leftFlatMap(_ => x.as[Swap])
      .leftFlatMap(_ => x.as[SwapMultiAddress])

  implicit val showCFMMOrder: Show[CFMMOrder.Any] = {
    case d: Deposit          => d.show
    case r: Redeem           => r.show
    case s: Swap             => s.show
    case s: SwapMultiAddress => s.show
  }

  implicit val showSwapAny: Show[SwapAny] = {
    case s: Swap             => s.show
    case s: SwapMultiAddress => s.show
  }

  implicit val loggableCFMMOrder: Loggable[CFMMOrder.Any] = Loggable.show

  implicit val loggableSwapAny: Loggable[SwapAny] = Loggable.show

  @derive(show, loggable, encoder, decoder)
  final case class Deposit(poolId: PoolId, maxMinerFee: Long, timestamp: Long, params: DepositParams, box: Output)
    extends CFMMOrder[CFMMOrderType.Deposit] {
    val orderType: CFMMOrderType.Deposit = CFMMOrderType.deposit
  }

  @derive(show, loggable, encoder, decoder)
  final case class Redeem(poolId: PoolId, maxMinerFee: Long, timestamp: Long, params: RedeemParams, box: Output)
    extends CFMMOrder[CFMMOrderType.Redeem] {
    val orderType: CFMMOrderType.Redeem = CFMMOrderType.redeem
  }

  @derive(show, loggable)
  final case class Swap(
    poolId: PoolId,
    maxMinerFee: Long,
    timestamp: Long,
    params: SwapParams[PubKey],
    box: Output
  ) extends CFMMOrder[CFMMOrderType.P2Pk] {
    val orderType: CFMMOrderType.P2Pk = CFMMOrderType.swapP2Pk
  }

  object Swap {

    implicit val encoderSwap: Encoder[Swap] =
      deriveEncoder.mapJsonObject(_.add("orderType", CFMMOrderType.swapP2Pk.asJson))

    implicit val decoderSwap: Decoder[Swap] =
      deriveDecoder
        .validate(
          _.downField("orderType").as[CFMMOrderType.P2Pk].toOption.nonEmpty,
          "Incorrect swap p2pk order. There is no order type in json."
        )
  }

  @derive(show, loggable)
  final case class SwapMultiAddress(
    poolId: PoolId,
    maxMinerFee: Long,
    timestamp: Long,
    params: SwapParams[SErgoTree],
    box: Output
  ) extends CFMMOrder[CFMMOrderType.MultiAddress] {
    val orderType: CFMMOrderType.MultiAddress = CFMMOrderType.swapMultiAddress
  }

  object SwapMultiAddress {

    implicit val encoderSwapMultiAddress: Encoder[SwapMultiAddress] =
      deriveEncoder.mapJsonObject(_.add("orderType", CFMMOrderType.swapMultiAddress.asJson))

    implicit val decoderSwapMultiAddress: Decoder[SwapMultiAddress] =
      deriveDecoder
        .validate(
          _.downField("orderType").as[CFMMOrderType.MultiAddress].toOption.nonEmpty,
          "Incorrect swap multi address order. There is no order type in json."
        )
  }
}
