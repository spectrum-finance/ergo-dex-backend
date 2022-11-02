package org.ergoplatform.dex.domain.amm

import cats.Show
import io.circe.{Codec, Decoder, Encoder}
import tofu.logging.Loggable

sealed trait CFMMOrderType

object CFMMOrderType {

  sealed trait Swap extends CFMMOrderType

  sealed trait MultiAddress extends Swap

  object MultiAddress {

    implicit val show: Show[MultiAddress] = _ => "SwapMultiAddress"

    implicit val loggable: Loggable[MultiAddress] = Loggable.show

    implicit val codecMultiAddressSwapType: Codec[MultiAddress] = Codec
      .from(Decoder[String], Encoder[String])
      .iemap(str =>
        Either.cond(str == "swapMultiAddress", swapMultiAddress, s"incorrect swap multi address type: $str")
      )(_ => "swapMultiAddress")
  }

  sealed trait P2Pk extends Swap

  object P2Pk {

    implicit val show: Show[P2Pk] = _ => "SwapP2Pk"

    implicit val loggable: Loggable[P2Pk] = Loggable.show

    implicit val codecP2PkSwapType: Codec[P2Pk] = Codec
      .from(Decoder[String], Encoder[String])
      .iemap(str => Either.cond(str == "swapP2Pk", swapP2Pk, s"incorrect swap p2pk type: $str"))(_ => "swapP2Pk")
  }

  sealed abstract class Redeem extends CFMMOrderType

  object Redeem {

    implicit val show: Show[Redeem] = _ => "Redeem"

    implicit val loggable: Loggable[Redeem] = Loggable.show

    implicit val codecRedeemType: Codec[Redeem] = Codec
      .from(Decoder[String], Encoder[String])
      .iemap(str => Either.cond(str == "redeem", redeem, s"incorrect redeem type: $str"))(_ => "redeem")
  }

  sealed abstract class Deposit extends CFMMOrderType

  object Deposit {

    implicit val show: Show[Deposit] = _ => "Deposit"

    implicit val loggable: Loggable[Deposit] = Loggable.show

    implicit val codecDepositType: Codec[Deposit] = Codec
      .from(Decoder[String], Encoder[String])
      .iemap(str => Either.cond(str == "deposit", deposit, s"incorrect deposit type: $str"))(_ => "deposit")
  }

  type Any = CFMMOrderType

  def deposit: Deposit               = new Deposit {}
  def redeem: Redeem                 = new Redeem {}
  def swap: Swap                     = new Swap {}
  def swapMultiAddress: MultiAddress = new MultiAddress {}
  def swapP2Pk: P2Pk                 = new P2Pk {}
}
