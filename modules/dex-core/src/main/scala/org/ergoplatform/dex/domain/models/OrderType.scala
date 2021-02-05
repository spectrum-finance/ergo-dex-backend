package org.ergoplatform.dex.domain.models

import cats.Show
import cats.instances.string._
import cats.syntax.either._
import doobie.{Get, Put}
import enumeratum._
import io.circe.{Decoder, Encoder}
import tofu.logging.Loggable

import scala.collection.immutable

sealed trait OrderType extends EnumEntry {
  def isAsk: Boolean = false
}

object OrderType extends Enum[OrderType] with CirceEnum[OrderType] {

  case object Ask extends OrderType { override def isAsk: Boolean = true }
  case object Bid extends OrderType

  type Ask = Ask.type
  type Bid = Bid.type

  val values: immutable.IndexedSeq[OrderType] = findValues

  implicit val get: Get[OrderType] =
    Get[String].temap(s => withNameInsensitiveEither(s).leftMap(_.getMessage))

  implicit val put: Put[OrderType] =
    Put[String].contramap[OrderType](_.entryName)

  implicit val show: Show[OrderType] = _.entryName

  implicit val loggable: Loggable[OrderType] = Loggable.show
}
