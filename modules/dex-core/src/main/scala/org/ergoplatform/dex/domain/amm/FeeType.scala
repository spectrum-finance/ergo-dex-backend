package org.ergoplatform.dex.domain.amm

import cats.syntax.either._
import io.circe.{Decoder, Encoder}

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
