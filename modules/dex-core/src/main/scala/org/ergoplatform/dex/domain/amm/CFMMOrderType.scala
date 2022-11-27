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

  sealed abstract class FeeType

  object FeeType {

    sealed abstract class ErgFee extends FeeType

    object ErgFee {
      implicit val encoderErgFee: Encoder[ErgFee] = Encoder[String].contramap(_ => "ergFee")

      implicit val decoderErgFee: Decoder[ErgFee] = Decoder[String].emap {
        case "ergFee" => ergFee.asRight
        case nonsense => s"Invalid type in ErgFee: $nonsense".asLeft
      }
    }

    sealed abstract class TokenFee extends FeeType

    object TokenFee {
      implicit val encoderTokenFee: Encoder[TokenFee] = Encoder[String].contramap(_ => "tokenFee")

      implicit val decoderTokenFee: Decoder[TokenFee] = Decoder[String].emap {
        case "tokenFee" => tokenFee.asRight
        case nonsense   => s"Invalid type in TokenFee: $nonsense".asLeft
      }
    }

    def tokenFee: TokenFee = new TokenFee {}

    def ergFee: ErgFee = new ErgFee {}
  }

  sealed trait RedeemType extends CFMMOrderType

  object RedeemType {
    def redeemType: RedeemType = new RedeemType {}
  }

  sealed trait DepositType extends CFMMOrderType

  object DepositType {
    def depositType: DepositType = new DepositType {}
  }
}
