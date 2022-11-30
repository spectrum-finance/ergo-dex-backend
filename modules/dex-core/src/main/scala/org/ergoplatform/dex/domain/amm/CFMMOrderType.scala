package org.ergoplatform.dex.domain.amm

import cats.syntax.either._
import io.circe.{Decoder, Encoder}

sealed trait CFMMOrderType

object CFMMOrderType {

  sealed trait SwapType extends CFMMOrderType

  object SwapType {

    sealed trait SwapErgFee extends SwapType

    trait SwapMultiAddress extends SwapErgFee

    object SwapMultiAddress {

      implicit val encoderSwapMultiAddress: Encoder[SwapMultiAddress] =
        Encoder[String].contramap(_ => "swapMultiAddress")

      implicit val decoderSwapMultiAddress: Decoder[SwapMultiAddress] = Decoder[String].emap {
        case "swapMultiAddress" => swapMultiAddress.asRight
        case nonsense           => s"Invalid type in SwapMultiAddress: $nonsense".asLeft
      }
    }

    trait SwapP2Pk extends SwapErgFee

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

  sealed trait RedeemType extends CFMMOrderType

  object RedeemType {
    sealed trait RedeemErgFee extends RedeemType

    object RedeemErgFee {

      implicit val encoder: Encoder[RedeemErgFee] =
        Encoder[String].contramap(_ => "redeemErgFee")

      implicit val decoder: Decoder[RedeemErgFee] = Decoder[String].emap {
        case "redeemErgFee" => redeemErgFee.asRight
        case nonsense       => s"Invalid type in RedeemErgFee: $nonsense".asLeft
      }
    }

    sealed trait RedeemTokenFee extends RedeemType

    object RedeemTokenFee {

      implicit val encoder: Encoder[RedeemTokenFee] =
        Encoder[String].contramap(_ => "redeemTokenFee")

      implicit val decoder: Decoder[RedeemTokenFee] = Decoder[String].emap {
        case "redeemTokenFee" => redeemTokenFee.asRight
        case nonsense         => s"Invalid type in RedeemTokenFee: $nonsense".asLeft
      }
    }

    def redeemErgFee: RedeemErgFee = new RedeemErgFee {}

    def redeemTokenFee: RedeemTokenFee = new RedeemTokenFee {}
  }

  sealed trait DepositType extends CFMMOrderType

  object DepositType {

    sealed trait DepositErgFee extends DepositType

    object DepositErgFee {

      implicit val encoder: Encoder[DepositErgFee] =
        Encoder[String].contramap(_ => "depositErgFee")

      implicit val decoder: Decoder[DepositErgFee] = Decoder[String].emap {
        case "depositErgFee" => depositErgFee.asRight
        case nonsense        => s"Invalid type in DepositErgFee: $nonsense".asLeft
      }
    }

    sealed trait DepositTokenFee extends DepositType

    object DepositTokenFee {

      implicit val encoder: Encoder[DepositTokenFee] =
        Encoder[String].contramap(_ => "depositTokenFee")

      implicit val decoder: Decoder[DepositTokenFee] = Decoder[String].emap {
        case "depositTokenFee" => depositTokenFee.asRight
        case nonsense          => s"Invalid type in DepositTokenFee: $nonsense".asLeft
      }
    }

    def depositErgFee: DepositErgFee     = new DepositErgFee {}
    def depositTokenFee: DepositTokenFee = new DepositTokenFee {}
  }
}
