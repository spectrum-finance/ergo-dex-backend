package org.ergoplatform.dex.domain.amm

import cats.Show
import cats.syntax.either._
import cats.syntax.show._
import derevo.cats.show
import derevo.circe.{decoder, encoder}
import derevo.derive
import io.circe.derivation.{deriveDecoder, deriveEncoder}
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import org.ergoplatform.dex.domain.amm.CFMMOrderType.FeeType.{ErgFee, TokenFee}
import org.ergoplatform.dex.domain.amm.CFMMOrderType.SwapType._
import org.ergoplatform.dex.domain.amm.CFMMOrderType.{FeeType, SwapType}
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

  type Any        = CFMMOrder[CFMMOrderType]
  type SwapAny    = CFMMOrder[CFMMOrderType.SwapType]
  type DepositAny = CFMMOrder[CFMMOrderType.FeeType]

  implicit def decoderAny: Decoder[Any] = x =>
    x.as[Deposit[ErgFee, PubKey]]
      .leftFlatMap(_ => x.as[Deposit[TokenFee, SErgoTree]])
      .leftFlatMap(_ => x.as[Redeem[TokenFee, SErgoTree]])
      .leftFlatMap(_ => x.as[Redeem[ErgFee, PubKey]])
      .leftFlatMap(_ => x.as[SwapTokenFee])
      .leftFlatMap(_ => x.as[Swap[SwapP2Pk, PubKey]])
      .leftFlatMap(_ => x.as[Swap[SwapMultiAddress, SErgoTree]])

  implicit def encoderAny: Encoder[Any] = Encoder.instance {
    case value: Deposit[FeeType.ErgFee, PubKey]      => value.asJson
    case value: Deposit[FeeType.TokenFee, SErgoTree] => value.asJson
    case value: Redeem[ErgFee, PubKey]               => value.asJson
    case value: Redeem[TokenFee, SErgoTree]          => value.asJson
    case value: SwapTokenFee                         => value.asJson
    case value: Swap[SwapP2Pk, PubKey]               => value.asJson
    case value: Swap[SwapMultiAddress, SErgoTree]    => value.asJson
  }

  implicit val showAny: Show[Any] = {
    case d: Deposit[ErgFee, PubKey]           => d.show
    case d: Deposit[TokenFee, SErgoTree]      => d.show
    case r: Redeem[TokenFee, SErgoTree]       => r.show
    case r: Redeem[ErgFee, PubKey]            => r.show
    case s: SwapTokenFee                      => s.show
    case s: Swap[SwapP2Pk, PubKey]            => s.show
    case s: Swap[SwapMultiAddress, SErgoTree] => s.show
  }

  implicit val showSwapAny: Show[SwapAny] = {
    case s: Swap[SwapP2Pk, PubKey]            => s.show
    case s: Swap[SwapMultiAddress, SErgoTree] => s.show
    case s: SwapTokenFee                      => s.show
  }

  implicit val loggableCFMMOrder: Loggable[CFMMOrder.Any] = Loggable.show

  implicit val loggableSwapAny: Loggable[SwapAny] = Loggable.show

  /** @param params If orderType is DepositTokenFee and inY or inX are spectrum
    *               tokens, then inY or inX are already without dex fee. Otherwise
    *               they keep original value.
    *               If orderType is DepositErgFee, inX original value preserved.
    */
  final case class Deposit[+O <: FeeType, T](
    poolId: PoolId,
    maxMinerFee: Long,
    timestamp: Long,
    params: DepositParams[T],
    box: Output,
    orderType: O
  ) extends CFMMOrder[O]

  object Deposit {

    implicit def showDeposit[O <: FeeType, T]: Show[Deposit[O, T]] = _.toString

    implicit def loggableDeposit[O <: FeeType, T]: Loggable[Deposit[O, T]] = Loggable.show

    implicit def encoderDeposit[O <: FeeType: Encoder, T: Encoder]: Encoder[Deposit[O, T]] = deriveEncoder

    implicit def decoderDeposit[O <: FeeType: Decoder, T: Decoder]: Decoder[Deposit[O, T]] = deriveDecoder
  }

  final case class Redeem[+O <: FeeType, T](
    poolId: PoolId,
    maxMinerFee: Long,
    timestamp: Long,
    params: RedeemParams[T],
    box: Output,
    orderType: O
  ) extends CFMMOrder[O]

  object Redeem {

    implicit def showRedeem[O <: FeeType, T]: Show[Redeem[O, T]] = _.toString

    implicit def loggableRedeem[O <: FeeType, T]: Loggable[Redeem[O, T]] = Loggable.show

    implicit def encoderRedeem[O <: FeeType: Encoder, T: Encoder]: Encoder[Redeem[O, T]] = deriveEncoder

    implicit def decoderRedeem[O <: FeeType: Decoder, T: Decoder]: Decoder[Redeem[O, T]] = deriveDecoder
  }

  final case class Swap[+O <: SwapType, T](
    poolId: PoolId,
    maxMinerFee: Long,
    timestamp: Long,
    params: SwapParams[T],
    box: Output,
    orderType: O
  ) extends CFMMOrder[O]

  object Swap {

    implicit def showSwap[O <: SwapType, T]: Show[Swap[O, T]] = _.toString

    implicit def loggableSwap[O <: SwapType, T]: Loggable[Swap[O, T]] = Loggable.show

    implicit def encoderSwap[O <: SwapType: Encoder, T: Encoder]: Encoder[Swap[O, T]] = deriveEncoder

    implicit def decoderSwap[O <: SwapType: Decoder, T: Decoder]: Decoder[Swap[O, T]] = deriveDecoder
  }

  /**
    *
    * @param params baseAmount is already without max dex fee if needed
    */
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
}
