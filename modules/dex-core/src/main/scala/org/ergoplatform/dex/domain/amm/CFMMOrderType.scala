package org.ergoplatform.dex.domain.amm

import cats.syntax.either._
import derevo.circe.{decoder, encoder}
import derevo.derive
import io.circe.{Decoder, Encoder}

sealed trait CFMMOrderType

object CFMMOrderType {

  sealed trait SwapType extends CFMMOrderType

  object SwapType {

    trait SwapMultiAddress extends SwapType

    object SwapMultiAddress {

      implicit val encoderSwapMultiAddress: Encoder[SwapMultiAddress] =
        Encoder[String].contramap(_ => "swapMultiAddress")

      implicit val decoderSwapMultiAddress: Decoder[SwapMultiAddress] = Decoder[String].emap {
        case "swapMultiAddress" => swapMultiAddress.asRight
        case nonsense           => s"Invalid type in SwapMultiAddress: $nonsense".asLeft
      }
    }

    trait SwapP2Pk extends SwapType

    object SwapP2Pk {
      implicit val encoderSwapP2Pk: Encoder[SwapP2Pk] = Encoder[String].contramap(_ => "swapP2Pk")

      implicit val decoderSwapP2Pk: Decoder[SwapP2Pk] = Decoder[String].emap {
        case "swapP2Pk" => swapP2Pk.asRight
        case nonsense   => s"Invalid type in SwapP2Pk: $nonsense".asLeft
      }
    }


    trait SwapTokenFee extends SwapType

    def swapMultiAddress: SwapMultiAddress = new SwapMultiAddress {}

    def swapP2Pk: SwapP2Pk = new SwapP2Pk {}

    def swapTokenFee: SwapTokenFee = new SwapTokenFee {}
  }

  sealed abstract class DepositType extends CFMMOrderType

  object DepositType {

    sealed abstract class DepositErgFee extends DepositType

    sealed abstract class DepositTokenFee extends DepositType

    def depositTokenFee: DepositTokenFee = new DepositTokenFee {}

    def depositErgFee: DepositErgFee     = new DepositErgFee {}
  }

  sealed abstract class RedeemType extends CFMMOrderType

  object RedeemType {

    sealed abstract class RedeemErgFee extends RedeemType

    sealed abstract class RedeemTokenFee extends RedeemType

    def redeemTokenFee: RedeemTokenFee = new RedeemTokenFee {}

    def redeemErgFee: RedeemErgFee = new RedeemErgFee {}
  }
}
