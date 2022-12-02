package org.ergoplatform.dex.markets.api.v1.models.amm

import cats.Show
import derevo.circe.{decoder, encoder}
import derevo.derive
import enumeratum.{CirceEnum, Enum, EnumEntry}
import sttp.tapir.{Schema, Validator}
import tofu.logging.Loggable
import scala.collection.immutable

sealed trait OrderStatus extends EnumEntry

@derive(encoder, decoder)
object OrderStatus extends Enum[OrderStatus] with CirceEnum[OrderStatus] {

  case object Pending extends OrderStatus
  case object Registered extends OrderStatus
  case object Executed extends OrderStatus
  case object Locked extends OrderStatus
  case object NeedRefund extends OrderStatus
  case object Refunded extends OrderStatus

  val values: immutable.IndexedSeq[OrderStatus] = findValues

  implicit val show: Show[OrderStatus]         = _.entryName
  implicit val loggable: Loggable[OrderStatus] = Loggable.stringValue.contramap(_.entryName)

  implicit val schema: Schema[OrderStatus] =
    Schema.string.validate(Validator.enumeration(OrderStatus.values.toList, v => Option(v)))
}
